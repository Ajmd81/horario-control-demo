package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.ComputoDtos.*;
import com.controlhorario.lite.entity.Empleado;
import com.controlhorario.lite.entity.Fichaje;
import com.controlhorario.lite.repository.EmpleadoRepository;
import com.controlhorario.lite.repository.FichajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComputoService {

    private final FichajeRepository fichajeRepo;
    private final EmpleadoRepository empleadoRepo;

    public static final int LIMITE_ANUAL_EXTRAS = 80;     // Art. 35.2 ET
    public static final int FRANJA_DIURNA_INI   = 6;       // 06:00
    public static final int FRANJA_DIURNA_FIN   = 22;      // 22:00

    /** Cómputo individual de un empleado en un rango. */
    public ComputoEmpleadoResponse computarEmpleado(Long empleadoId, LocalDate desde, LocalDate hasta) {
        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));

        double horasContratadasDia = emp.getHorasContratadasMin() / 60.0;

        List<Fichaje> fichajes = fichajeRepo.findCerradosByEmpleadoAndRango(
                empleadoId,
                desde.atStartOfDay(),
                hasta.atTime(LocalTime.MAX));

        // Agrupar por fecha
        Map<LocalDate, List<Fichaje>> porFecha = fichajes.stream()
                .collect(Collectors.groupingBy(f -> f.getHoraEntrada().toLocalDate()));

        List<DiaComputo> dias = porFecha.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> calcularDia(e.getKey(), e.getValue(), horasContratadasDia))
                .toList();

        // Resumen semanal — semana actual (lunes a domingo) dentro del rango
        ResumenPeriodo semanal = resumir(filtrarSemanaActual(dias));
        ResumenPeriodo mensual = resumir(dias);

        // Extras acumuladas en el año natural
        double extrasAnio = calcularExtrasAcumuladasAnio(empleadoId, desde.getYear(), horasContratadasDia);

        return new ComputoEmpleadoResponse(
                emp.getId(),
                emp.getNombre() + " " + emp.getApellido(),
                horasContratadasDia,
                extrasAnio,
                LIMITE_ANUAL_EXTRAS,
                dias,
                semanal,
                mensual
        );
    }

    /** Tabla resumen del equipo en un rango. */
    public List<EmpleadoComputoEquipo> computarEquipo(Long empresaId, LocalDate desde, LocalDate hasta) {
        List<Fichaje> fichajes = fichajeRepo.findCerradosByEmpresaAndRango(
                empresaId,
                desde.atStartOfDay(),
                hasta.atTime(LocalTime.MAX));

        Map<Long, List<Fichaje>> porEmpleado = fichajes.stream()
                .collect(Collectors.groupingBy(f -> f.getEmpleado().getId()));

        // Incluir también empleados sin fichajes en el periodo
        List<Empleado> empleados = empleadoRepo.findByEmpresaIdAndActivoTrue(empresaId);

        return empleados.stream().map(emp -> {
            double horasContratadasDia = emp.getHorasContratadasMin() / 60.0;
            List<Fichaje> fs = porEmpleado.getOrDefault(emp.getId(), List.of());

            Map<LocalDate, List<Fichaje>> porFecha = fs.stream()
                    .collect(Collectors.groupingBy(f -> f.getHoraEntrada().toLocalDate()));

            List<DiaComputo> dias = porFecha.entrySet().stream()
                    .map(e -> calcularDia(e.getKey(), e.getValue(), horasContratadasDia))
                    .toList();

            ResumenPeriodo r = resumir(dias);
            double extrasAnio = calcularExtrasAcumuladasAnio(emp.getId(), desde.getYear(), horasContratadasDia);

            return new EmpleadoComputoEquipo(
                    emp.getId(),
                    emp.getNombre() + " " + emp.getApellido(),
                    horasContratadasDia,
                    r.horasOrdinarias(),
                    r.horasExtrasDiurnas(),
                    r.horasExtrasNocturnas(),
                    r.totalHoras(),
                    extrasAnio,
                    extrasAnio >= LIMITE_ANUAL_EXTRAS
            );
        }).toList();
    }

    /** Total de horas extras acumuladas en el año natural (para el bloqueo). */
    public double calcularExtrasAcumuladasAnio(Long empleadoId, int anio, double horasContratadasDia) {
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);

        List<Fichaje> fichajes = fichajeRepo.findCerradosByEmpleadoAndRango(
                empleadoId, desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));

        Map<LocalDate, List<Fichaje>> porFecha = fichajes.stream()
                .collect(Collectors.groupingBy(f -> f.getHoraEntrada().toLocalDate()));

        return porFecha.entrySet().stream()
                .mapToDouble(e -> {
                    DiaComputo d = calcularDia(e.getKey(), e.getValue(), horasContratadasDia);
                    return d.horasExtrasDiurnas() + d.horasExtrasNocturnas();
                })
                .sum();
    }

    // ─── Cálculo por día ─────────────────────────────────────────────────────
    private DiaComputo calcularDia(LocalDate fecha, List<Fichaje> fichajes, double horasContratadasDia) {
        double minutosDiurnos = 0;
        double minutosNocturnos = 0;

        for (Fichaje f : fichajes) {
            if (f.getHoraSalida() == null) continue;
            double[] segmento = minutosDiurnoYNocturno(f.getHoraEntrada(), f.getHoraSalida());
            minutosDiurnos   += segmento[0];
            minutosNocturnos += segmento[1];
        }

        double horasTotales = (minutosDiurnos + minutosNocturnos) / 60.0;
        double horasOrdinarias = Math.min(horasTotales, horasContratadasDia);
        double horasExtras = Math.max(0, horasTotales - horasContratadasDia);

        // Repartir extras proporcionalmente entre diurnas y nocturnas
        double horasExtrasDiurnas = 0, horasExtrasNocturnas = 0;
        if (horasExtras > 0 && (minutosDiurnos + minutosNocturnos) > 0) {
            double ratioNocturno = minutosNocturnos / (minutosDiurnos + minutosNocturnos);
            horasExtrasNocturnas = horasExtras * ratioNocturno;
            horasExtrasDiurnas   = horasExtras - horasExtrasNocturnas;
        }

        return new DiaComputo(
                fecha,
                round2(horasOrdinarias),
                round2(horasExtrasDiurnas),
                round2(horasExtrasNocturnas),
                round2(horasTotales)
        );
    }

    /** Divide los minutos de un fichaje entre franja diurna (06–22) y nocturna (22–06). */
    private double[] minutosDiurnoYNocturno(LocalDateTime ini, LocalDateTime fin) {
        long totalMin = Duration.between(ini, fin).toMinutes();
        if (totalMin <= 0) return new double[]{0, 0};

        double diurnos = 0, nocturnos = 0;
        LocalDateTime cursor = ini;

        while (cursor.isBefore(fin)) {
            LocalDateTime siguiente = cursor.plusMinutes(1);
            int hora = cursor.getHour();
            if (hora >= FRANJA_DIURNA_INI && hora < FRANJA_DIURNA_FIN) {
                diurnos++;
            } else {
                nocturnos++;
            }
            cursor = siguiente;
        }
        return new double[]{diurnos, nocturnos};
    }

    private List<DiaComputo> filtrarSemanaActual(List<DiaComputo> dias) {
        WeekFields wf = WeekFields.of(Locale.forLanguageTag("es-ES"));
        int semanaActual = LocalDate.now().get(wf.weekOfYear());
        return dias.stream()
                .filter(d -> d.fecha().get(wf.weekOfYear()) == semanaActual)
                .toList();
    }

    private ResumenPeriodo resumir(List<DiaComputo> dias) {
        double ord = dias.stream().mapToDouble(DiaComputo::horasOrdinarias).sum();
        double exD = dias.stream().mapToDouble(DiaComputo::horasExtrasDiurnas).sum();
        double exN = dias.stream().mapToDouble(DiaComputo::horasExtrasNocturnas).sum();
        return new ResumenPeriodo(round2(ord), round2(exD), round2(exN), round2(exD + exN), round2(ord + exD + exN));
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}