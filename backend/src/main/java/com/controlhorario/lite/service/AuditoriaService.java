package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.AuditoriaFichajeRepository;
import com.controlhorario.lite.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaFichajeRepository auditoriaRepo;
    private final UsuarioRepository usuarioRepo;
    private final HashService hashService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registrarCreacion(Fichaje fichaje, Long usuarioId) {
        String json = snapshotFichaje(fichaje);
        guardar(fichaje.getId(), AuditoriaFichaje.Accion.CREATE,
                null, json, "Fichaje creado", usuarioId);
    }

    public void registrarModificacion(Long fichajeId, String valorAntes, String valorDespues,
                                       String motivo, Long usuarioId) {
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("El motivo de modificación es obligatorio");
        guardar(fichajeId, AuditoriaFichaje.Accion.UPDATE,
                valorAntes, valorDespues, motivo, usuarioId);
    }

    public void registrarEliminacion(Long fichajeId, String valorAntes, String motivo, Long usuarioId) {
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("El motivo de eliminación es obligatorio");
        guardar(fichajeId, AuditoriaFichaje.Accion.DELETE,
                valorAntes, null, motivo, usuarioId);
    }

    public List<AuditoriaFichaje> historial(Long fichajeId) {
        return auditoriaRepo.findByFichajeIdOrderByTimestampDesc(fichajeId);
    }

    public String snapshotFichaje(Fichaje f) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "id",            f.getId(),
                "horaEntrada",   f.getHoraEntrada(),
                "horaSalida",    f.getHoraSalida() != null ? f.getHoraSalida() : "",
                "latitud",       f.getLatitud()    != null ? f.getLatitud()    : "",
                "longitud",      f.getLongitud()   != null ? f.getLongitud()   : "",
                "tipo",          f.getTipo().name(),
                "cerrado",       f.isCerrado(),
                "version",       f.getVersion(),
                "observaciones", f.getObservaciones() != null ? f.getObservaciones() : ""
            ));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void guardar(Long fichajeId, AuditoriaFichaje.Accion accion,
                          String antes, String despues, String motivo, Long usuarioId) {
        Usuario u = usuarioId != null ? usuarioRepo.findById(usuarioId).orElse(null) : null;
        String hash = hashService.calcularHashAuditoria(
                fichajeId, accion.name(), antes, despues, motivo, usuarioId);

        AuditoriaFichaje aud = AuditoriaFichaje.builder()
                .fichajeId(fichajeId)
                .accion(accion)
                .valorAntes(antes)
                .valorDespues(despues)
                .motivo(motivo)
                .modificadoPor(u)
                .timestamp(LocalDateTime.now())
                .hash(hash)
                .build();
        auditoriaRepo.save(aud);
    }
}
