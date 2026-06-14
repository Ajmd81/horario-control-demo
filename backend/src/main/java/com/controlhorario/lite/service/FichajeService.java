package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.*;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FichajeService {

    private final FichajeRepository fichajeRepo;
    private final EmpleadoRepository empleadoRepo;
    private final ComputoService computoService;
    private final VacacionesService vacacionesService;
    private final HashService hashService;
    private final AuditoriaService auditoriaService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public FichajeResponse entrada(Long empleadoId, FichajeEntradaRequest req) {
        if (fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes un fichaje abierto. Registra la salida primero.");

        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // ── Bloqueo por vacaciones aprobadas ──────────────────────────
        if (vacacionesService.estaEnVacaciones(empleadoId, LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "VACACIONES_APROBADAS: Hoy estás de vacaciones aprobadas.");

        // ── Bloqueo por límite anual extras (Art. 35.2 ET) ────────────
        double horasContratadasDia = emp.getHorasContratadasMin() / 60.0;
        double extrasAnio = computoService.calcularExtrasAcumuladasAnio(
                empleadoId, LocalDate.now().getYear(), horasContratadasDia);
        if (extrasAnio >= ComputoService.LIMITE_ANUAL_EXTRAS)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "LIMITE_ANUAL_EXTRAS: Has alcanzado las 80 horas extras anuales legales.");

        // ── Crear fichaje ─────────────────────────────────────────────
        Fichaje.Tipo tipo = req.tipo() != null ? req.tipo() : Fichaje.Tipo.JORNADA;

        Fichaje f = Fichaje.builder()
                .horaEntrada(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .latitud(req.latitud())
                .longitud(req.longitud())
                .cerrado(false)
                .tipo(tipo)
                .observaciones(req.observaciones())
                .mocked(req.mocked() != null && req.mocked())
                .empleado(emp)
                .version(1)
                .build();

        // ── Hash chain ────────────────────────────────────────────────
        String hashAnterior = obtenerUltimoHash(empleadoId);
        f.setHashAnterior(hashAnterior);
        f.setHashActual(hashService.calcularHashFichaje(f, hashAnterior));

        Fichaje saved = fichajeRepo.save(f);

        // ── Auditoría ─────────────────────────────────────────────────
        auditoriaService.registrarCreacion(saved, emp.getUsuario() != null ? emp.getUsuario().getId() : null);

        return toResponse(saved);
    }

    @Transactional
    public FichajeResponse salida(Long empleadoId) {
        Fichaje f = fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay fichaje abierto"));

        f.setHoraSalida(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        f.setCerrado(true);
        // NO incrementamos versión: el cierre forma parte del ciclo natural del fichaje.
        // La versión solo se incrementa en modificaciones manuales (PATCH /fichajes/{id}).
        f.setHashActual(hashService.calcularHashFichaje(f, f.getHashAnterior()));

        return toResponse(fichajeRepo.save(f));
    }

    /** Modificación admin con motivo obligatorio + audit log. */
    @Transactional
    public FichajeResponse modificar(Long fichajeId, FichajeModificarRequest req, Long usuarioId) {
        if (req.motivo() == null || req.motivo().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El motivo de modificación es obligatorio");

        Fichaje f = fichajeRepo.findById(fichajeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String antes = auditoriaService.snapshotFichaje(f);

        if (req.horaEntrada()   != null) f.setHoraEntrada(req.horaEntrada().truncatedTo(ChronoUnit.SECONDS));
        if (req.horaSalida()    != null) f.setHoraSalida(req.horaSalida().truncatedTo(ChronoUnit.SECONDS));
        if (req.tipo()          != null) f.setTipo(req.tipo());
        if (req.observaciones() != null) f.setObservaciones(req.observaciones());

        f.setVersion(f.getVersion() + 1);
        f.setHashActual(hashService.calcularHashFichaje(f, f.getHashAnterior()));

        Fichaje saved = fichajeRepo.save(f);
        String despues = auditoriaService.snapshotFichaje(saved);

        auditoriaService.registrarModificacion(fichajeId, antes, despues, req.motivo(), usuarioId);

        return toResponse(saved);
    }

    public List<FichajeResponse> misFichajes(Long empleadoId) {
        return fichajeRepo.findByEmpleadoId(empleadoId)
                .stream().map(this::toResponse).toList();
    }

    public List<FichajeResponse> todosEnEmpresa(Long empresaId) {
        return fichajeRepo.findByEmpresaId(empresaId)
                .stream().map(this::toResponse).toList();
    }

    public FichajeResponse fichajeActivo(Long empleadoId) {
        return fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin fichaje activo"));
    }

    private String obtenerUltimoHash(Long empleadoId) {
        return fichajeRepo.findByEmpleadoId(empleadoId).stream()
                .findFirst()  // ordenados DESC, el más reciente
                .map(Fichaje::getHashActual)
                .orElse(null);
    }

    private FichajeResponse toResponse(Fichaje f) {
        return new FichajeResponse(
                f.getId(),
                f.getHoraEntrada() != null ? f.getHoraEntrada().format(FMT) : null,
                f.getHoraSalida()  != null ? f.getHoraSalida().format(FMT)  : null,
                f.getLatitud(), f.getLongitud(),
                f.isCerrado(),
                f.getEmpleado().getId(),
                f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellido(),
                f.isMocked()
        );
    }
}