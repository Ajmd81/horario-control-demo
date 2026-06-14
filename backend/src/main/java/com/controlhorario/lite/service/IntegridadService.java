package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.IntegridadDtos.*;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegridadService {

    private final FichajeRepository                fichajeRepo;
    private final EmpresaRepository                empresaRepo;
    private final AuditoriaFichajeRepository       auditoriaRepo;
    private final VerificacionIntegridadRepository historialRepo;
    private final HashService                      hashService;

    /** Verifica la cadena hash de toda la empresa. Guarda resultado en histórico. */
    @Transactional
    public IntegridadResultado verificarEmpresa(Long empresaId, boolean automatica) {
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        List<Fichaje> todos = fichajeRepo.findByEmpresaId(empresaId);
        // Agrupar por empleado para verificar la cadena hash de cada uno
        Map<Long, List<Fichaje>> porEmpleado = todos.stream()
                .collect(Collectors.groupingBy(f -> f.getEmpleado().getId()));

        List<FichajeCorrupto> incidencias = new ArrayList<>();

        for (var entry : porEmpleado.entrySet()) {
            // Ordenar por hora de entrada ASC para verificar la cadena cronológicamente
            List<Fichaje> fichajes = entry.getValue().stream()
                    .sorted(Comparator.comparing(Fichaje::getHoraEntrada))
                    .toList();

            String hashEsperadoAnterior = null;
            for (Fichaje f : fichajes) {
                // 1) Comprobar hash actual coincide con datos
                String hashRecalculado = hashService.calcularHashFichaje(f, f.getHashAnterior());
                if (f.getHashActual() != null && !f.getHashActual().equals(hashRecalculado)) {
                    incidencias.add(new FichajeCorrupto(
                            f.getId(),
                            f.getEmpleado().getId(),
                            f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellido(),
                            f.getHoraEntrada(),
                            f.getVersion(),
                            TipoIncidencia.HASH_MISMATCH,
                            hashRecalculado,
                            f.getHashActual(),
                            "El hash almacenado no coincide con los datos actuales. Posible modificación directa en BBDD."
                    ));
                }

                // 2) Comprobar que hashAnterior coincide con el hash del fichaje previo
                if (hashEsperadoAnterior != null && f.getHashAnterior() != null
                        && !f.getHashAnterior().equals(hashEsperadoAnterior)) {
                    incidencias.add(new FichajeCorrupto(
                            f.getId(),
                            f.getEmpleado().getId(),
                            f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellido(),
                            f.getHoraEntrada(),
                            f.getVersion(),
                            TipoIncidencia.CADENA_ROTA,
                            hashEsperadoAnterior,
                            f.getHashAnterior(),
                            "El hash_anterior no coincide con el hash_actual del fichaje previo del empleado."
                    ));
                }

                // 3) Si version > 1, debe haber entradas UPDATE en auditoría
                if (f.getVersion() > 1) {
                    long updates = auditoriaRepo.findByFichajeIdOrderByTimestampDesc(f.getId()).stream()
                            .filter(a -> a.getAccion() == AuditoriaFichaje.Accion.UPDATE)
                            .count();
                    if (updates < (f.getVersion() - 1)) {
                        incidencias.add(new FichajeCorrupto(
                                f.getId(),
                                f.getEmpleado().getId(),
                                f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellido(),
                                f.getHoraEntrada(),
                                f.getVersion(),
                                TipoIncidencia.MODIFICACION_SIN_AUDIT,
                                null, null,
                                "El fichaje tiene versión " + f.getVersion() + " pero solo " + updates +
                                " entradas UPDATE en audit log. Modificación sin trazabilidad."
                        ));
                    }
                }

                hashEsperadoAnterior = f.getHashActual();
            }
        }

        IntegridadResultado resultado = new IntegridadResultado(
                empresaId,
                empresa.getNombre(),
                LocalDateTime.now(),
                todos.size(),
                incidencias.size(),
                incidencias.isEmpty(),
                incidencias
        );

        // Guardar en histórico
        historialRepo.save(VerificacionIntegridad.builder()
                .empresa(empresa)
                .fecha(LocalDateTime.now())
                .totalFichajes(todos.size())
                .totalCorruptos(incidencias.size())
                .automatica(automatica)
                .build());

        if (!incidencias.isEmpty()) {
            log.warn("Verificación integridad empresa {}: {} fichajes corruptos detectados",
                    empresa.getSlug(), incidencias.size());
        }

        return resultado;
    }

    /** Recalcula los hashes de TODOS los fichajes y pausas. */
    @Transactional
    public int recalcularHashes(Long empresaId) {
        List<Fichaje> todos = fichajeRepo.findByEmpresaId(empresaId);
        Map<Long, List<Fichaje>> porEmpleado = todos.stream()
                .collect(Collectors.groupingBy(f -> f.getEmpleado().getId()));

        int recalculados = 0;
        for (var entry : porEmpleado.entrySet()) {
            List<Fichaje> fichajes = entry.getValue().stream()
                    .sorted(Comparator.comparing(Fichaje::getHoraEntrada))
                    .toList();

            String hashAnterior = null;
            for (Fichaje f : fichajes) {
                // ── Corregir version sobrante del bug histórico del salida ──
                long updates = auditoriaRepo.findByFichajeIdOrderByTimestampDesc(f.getId()).stream()
                        .filter(a -> a.getAccion() == AuditoriaFichaje.Accion.UPDATE)
                        .count();
                int versionEsperada = 1 + (int) updates;
                if (f.getVersion() == versionEsperada + 1) {
                    f.setVersion(versionEsperada);
                }

                // ── Recalcular hash de la cadena ──
                f.setHashAnterior(hashAnterior);
                f.setHashActual(hashService.calcularHashFichaje(f, hashAnterior));
                hashAnterior = f.getHashActual();
                recalculados++;
            }
            fichajeRepo.saveAll(fichajes);
        }
        log.info("Recalculados {} hashes para empresa {}", recalculados, empresaId);
        return recalculados;
    }
    /** Versión anonimizada para RLT. */
    public IntegridadResumen verificarResumen(Long empresaId) {
        IntegridadResultado r = verificarEmpresa(empresaId, false);
        return new IntegridadResumen(r.fecha(), r.totalFichajes(), r.totalCorruptos(), r.cadenaIntegra());
    }

    public List<HistorialItem> historial(Long empresaId) {
        return historialRepo.findTop10ByEmpresaIdOrderByFechaDesc(empresaId).stream()
                .map(v -> new HistorialItem(
                        v.getId(),
                        v.getFecha(),
                        v.getTotalFichajes(),
                        v.getTotalCorruptos(),
                        v.getTotalCorruptos() == 0,
                        v.isAutomatica()
                ))
                .toList();
    }
}