package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.VacacionesDtos.*;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacacionesService {

    public static final int DIAS_ANUALES_DEFAULT = 22;

    private final VacacionesRepository vacacionesRepo;
    private final EmpleadoRepository empleadoRepo;
    private final UsuarioRepository usuarioRepo;

    @Transactional
    public VacacionesResponse solicitar(Long empleadoId, SolicitudRequest req) {
        if (req.fechaInicio() == null || req.fechaFin() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fechas obligatorias");
        if (req.fechaFin().isBefore(req.fechaInicio()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha fin anterior a inicio");
        if (req.fechaInicio().isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes solicitar vacaciones en el pasado");

        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

        List<Vacaciones> solapadas = vacacionesRepo.findSolapadas(empleadoId, req.fechaInicio(), req.fechaFin());
        if (!solapadas.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya tienes vacaciones solicitadas/aprobadas en esas fechas");

        int diasLab = contarDiasLaborables(req.fechaInicio(), req.fechaFin());

        DisponiblesResponse disp = calcularDisponibles(empleadoId, req.fechaInicio().getYear());
        if (diasLab > disp.diasDisponibles())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No tienes suficientes días disponibles (" + disp.diasDisponibles() + " restantes, solicitas " + diasLab + ")");

        Vacaciones v = Vacaciones.builder()
                .fechaInicio(req.fechaInicio())
                .fechaFin(req.fechaFin())
                .diasLaborables(diasLab)
                .estado(Vacaciones.Estado.PENDIENTE)
                .comentario(req.comentario())
                .fechaSolicitud(LocalDateTime.now())
                .empleado(emp)
                .build();
        return toResponse(vacacionesRepo.save(v));
    }

    public List<VacacionesResponse> misSolicitudes(Long empleadoId) {
        return vacacionesRepo.findByEmpleadoId(empleadoId)
                .stream().map(this::toResponse).toList();
    }

    public List<VacacionesResponse> solicitudesEmpresa(Long empresaId) {
        return vacacionesRepo.findByEmpresaId(empresaId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VacacionesResponse aprobar(Long vacacionesId, Long usuarioId) {
        Vacaciones v = vacacionesRepo.findById(vacacionesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (v.getEstado() != Vacaciones.Estado.PENDIENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitud ya resuelta");

        Usuario admin = usuarioRepo.findById(usuarioId).orElse(null);
        v.setEstado(Vacaciones.Estado.APROBADA);
        v.setFechaResolucion(LocalDateTime.now());
        v.setResueltoPor(admin);
        return toResponse(vacacionesRepo.save(v));
    }

    @Transactional
    public VacacionesResponse rechazar(Long vacacionesId, Long usuarioId, String motivo) {
        Vacaciones v = vacacionesRepo.findById(vacacionesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (v.getEstado() != Vacaciones.Estado.PENDIENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitud ya resuelta");

        Usuario admin = usuarioRepo.findById(usuarioId).orElse(null);
        v.setEstado(Vacaciones.Estado.RECHAZADA);
        v.setMotivoRechazo(motivo);
        v.setFechaResolucion(LocalDateTime.now());
        v.setResueltoPor(admin);
        return toResponse(vacacionesRepo.save(v));
    }

    @Transactional
    public void cancelar(Long vacacionesId, Long empleadoId) {
        Vacaciones v = vacacionesRepo.findById(vacacionesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!v.getEmpleado().getId().equals(empleadoId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es tu solicitud");
        if (v.getEstado() != Vacaciones.Estado.PENDIENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar solicitudes pendientes");
        vacacionesRepo.delete(v);
    }

    public DisponiblesResponse calcularDisponibles(Long empleadoId, int anio) {
        List<Vacaciones> delAnio = vacacionesRepo.findAprobadasByEmpleadoAndAnio(empleadoId, anio);
        int diasUsados = delAnio.stream().mapToInt(Vacaciones::getDiasLaborables).sum();

        int diasPendientes = vacacionesRepo.findByEmpleadoId(empleadoId).stream()
                .filter(v -> v.getEstado() == Vacaciones.Estado.PENDIENTE
                          && v.getFechaInicio().getYear() == anio)
                .mapToInt(Vacaciones::getDiasLaborables)
                .sum();

        int disponibles = DIAS_ANUALES_DEFAULT - diasUsados - diasPendientes;
        return new DisponiblesResponse(anio, DIAS_ANUALES_DEFAULT, diasUsados, diasPendientes, Math.max(0, disponibles));
    }

    /** True si la fecha cae en un periodo APROBADO. */
    public boolean estaEnVacaciones(Long empleadoId, LocalDate fecha) {
        return !vacacionesRepo.findAprobadasEnFecha(empleadoId, fecha).isEmpty();
    }

    private int contarDiasLaborables(LocalDate ini, LocalDate fin) {
        int count = 0;
        LocalDate d = ini;
        while (!d.isAfter(fin)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++;
            d = d.plusDays(1);
        }
        return count;
    }

    private VacacionesResponse toResponse(Vacaciones v) {
        return new VacacionesResponse(
                v.getId(),
                v.getEmpleado().getId(),
                v.getEmpleado().getNombre() + " " + v.getEmpleado().getApellido(),
                v.getFechaInicio(),
                v.getFechaFin(),
                v.getDiasLaborables(),
                v.getEstado(),
                v.getComentario(),
                v.getMotivoRechazo(),
                v.getFechaSolicitud(),
                v.getFechaResolucion(),
                v.getResueltoPor() != null ? v.getResueltoPor().getUsername() : null
        );
    }
}