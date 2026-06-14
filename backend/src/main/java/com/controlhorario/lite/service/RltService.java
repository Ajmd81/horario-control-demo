package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.RltDtos.*;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RltService {

    private final EmpresaRepository           empresaRepo;
    private final EmpleadoRepository          empleadoRepo;
    private final FichajeRepository           fichajeRepo;
    private final VacacionesRepository        vacacionesRepo;
    private final ComputoService              computoService;

    /** Genera un código anónimo determinista por empleado. */
    private String codigoAnonimo(Long empleadoId) {
        return "EMP-" + String.format("%03d", empleadoId);
    }

    public ResumenEmpresa resumenEmpresa(Long empresaId, int anio, int mes) {
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());
        LocalDateTime desdeLDT = desde.atStartOfDay();
        LocalDateTime hastaLDT = hasta.atTime(LocalTime.MAX);

        List<Empleado> empleados = empleadoRepo.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .filter(e -> e.getUsuario() == null || e.getUsuario().getRole() == Usuario.Role.EMPLOYEE)
                .toList();

        double ord = 0, extD = 0, extN = 0;
        int empleadosAlLimite = 0;
        for (Empleado e : empleados) {
            double horasContratadasDia = e.getHorasContratadasMin() / 60.0;
            var c = computoService.computarEmpleado(e.getId(), desde, hasta);
            ord += c.resumenMensual().horasOrdinarias();
            extD += c.resumenMensual().horasExtrasDiurnas();
            extN += c.resumenMensual().horasExtrasNocturnas();

            double extrasAnio = computoService.calcularExtrasAcumuladasAnio(
                    e.getId(), anio, horasContratadasDia);
            if (extrasAnio >= ComputoService.LIMITE_ANUAL_EXTRAS) empleadosAlLimite++;
        }

        int fichajesModificados = (int) fichajeRepo.findByEmpresaId(empresaId).stream()
                .filter(f -> f.getHoraEntrada().isAfter(desdeLDT) && f.getHoraEntrada().isBefore(hastaLDT))
                .filter(f -> f.getVersion() > 1)
                .count();

        List<Vacaciones> vacs = vacacionesRepo.findByEmpresaId(empresaId);
        int vacAprob = (int) vacs.stream()
                .filter(v -> v.getEstado() == Vacaciones.Estado.APROBADA)
                .filter(v -> v.getFechaInicio().getMonthValue() == mes && v.getFechaInicio().getYear() == anio)
                .count();
        int vacPend  = (int) vacs.stream().filter(v -> v.getEstado() == Vacaciones.Estado.PENDIENTE).count();
        int vacRech  = (int) vacs.stream().filter(v -> v.getEstado() == Vacaciones.Estado.RECHAZADA).count();

        double mediaHoras = empleados.isEmpty() ? 0 : (ord + extD + extN) / empleados.size();

        return new ResumenEmpresa(
                empresa.getNombre(),
                anio, mes,
                empleados.size(),
                empleados.size(),
                ord, extD, extN,
                mediaHoras,
                empleadosAlLimite,
                fichajesModificados,
                vacAprob, vacPend, vacRech
        );
    }

    public List<EmpleadoAnonimo> empleadosAnonimos(Long empresaId, int anio, int mes) {
        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());

        return empleadoRepo.findByEmpresaIdAndActivoTrue(empresaId).stream()
                .filter(e -> e.getUsuario() == null || e.getUsuario().getRole() == Usuario.Role.EMPLOYEE)
                .map(e -> {
                    double horasContratadasDia = e.getHorasContratadasMin() / 60.0;
                    var c = computoService.computarEmpleado(e.getId(), desde, hasta);
                    double extrasAnio = computoService.calcularExtrasAcumuladasAnio(
                            e.getId(), anio, horasContratadasDia);
                    return new EmpleadoAnonimo(
                            codigoAnonimo(e.getId()),
                            horasContratadasDia,
                            c.resumenMensual().horasOrdinarias(),
                            c.resumenMensual().horasExtrasDiurnas(),
                            c.resumenMensual().horasExtrasNocturnas(),
                            c.resumenMensual().totalHoras(),
                            extrasAnio,
                            extrasAnio >= ComputoService.LIMITE_ANUAL_EXTRAS
                    );
                })
                .sorted((a, b) -> a.codigoAnonimo().compareTo(b.codigoAnonimo()))
                .toList();
    }

    public List<FichajeAnonimo> fichajesAnonimos(Long empresaId, int anio, int mes) {
        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());

        return fichajeRepo.findByEmpresaId(empresaId).stream()
                .filter(f -> {
                    LocalDateTime he = f.getHoraEntrada();
                    return !he.toLocalDate().isBefore(desde) && !he.toLocalDate().isAfter(hasta);
                })
                .map(f -> new FichajeAnonimo(
                        f.getId(),
                        codigoAnonimo(f.getEmpleado().getId()),
                        f.getHoraEntrada(),
                        f.getHoraSalida(),
                        f.getTipo() != null ? f.getTipo().name() : "JORNADA",
                        f.isCerrado(),
                        f.getVersion(),
                        f.getVersion() > 1,
                        f.getLatitud() != null && f.getLongitud() != null
                ))
                .toList();
    }

    public List<VacacionesAnonima> vacacionesAnonimas(Long empresaId) {
        return vacacionesRepo.findByEmpresaId(empresaId).stream()
                .map(v -> new VacacionesAnonima(
                        v.getId(),
                        codigoAnonimo(v.getEmpleado().getId()),
                        v.getFechaInicio(),
                        v.getFechaFin(),
                        v.getDiasLaborables(),
                        v.getEstado().name(),
                        v.getFechaSolicitud(),
                        v.getFechaResolucion()
                ))
                .toList();
    }
}