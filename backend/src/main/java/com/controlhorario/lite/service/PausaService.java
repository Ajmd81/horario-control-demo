package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.PausaDtos.*;
import com.controlhorario.lite.entity.Fichaje;
import com.controlhorario.lite.entity.Pausa;
import com.controlhorario.lite.repository.FichajeRepository;
import com.controlhorario.lite.repository.PausaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PausaService {

    private final PausaRepository pausaRepo;
    private final FichajeRepository fichajeRepo;
    private final HashService hashService;

    @Transactional
    public PausaResponse iniciar(Long empleadoId, IniciarPausaRequest req) {
        Fichaje fichaje = fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay fichaje abierto. Inicia entrada antes de pausar."));

        // ── Idempotencia ──
        if (req.clientId() != null && !req.clientId().isBlank()) {
            var existente = pausaRepo.findByFichajeIdAndClientId(fichaje.getId(), req.clientId());
            if (existente.isPresent()) return toResponse(existente.get());
        }

        if (pausaRepo.findByFichajeIdAndHoraFinIsNull(fichaje.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una pausa activa. Reanuda antes de iniciar otra.");

        Pausa.Tipo tipo = req.tipo() != null ? req.tipo() : Pausa.Tipo.DESCANSO;
        boolean computa = tipo == Pausa.Tipo.INTERRUPCION;

        LocalDateTime horaInicio = (req.clientTimestamp() != null
                ? req.clientTimestamp()
                : LocalDateTime.now()).truncatedTo(ChronoUnit.SECONDS);

        Pausa p = Pausa.builder()
                .fichaje(fichaje)
                .horaInicio(horaInicio)
                .tipo(tipo)
                .computa(computa)
                .clientId(req.clientId())
                .build();

        String hashAnterior = pausaRepo.findByFichajeIdOrderByHoraInicioAsc(fichaje.getId())
                .stream().reduce((a, b) -> b)
                .map(Pausa::getHashActual)
                .orElse(fichaje.getHashActual());
        p.setHashAnterior(hashAnterior);
        p.setHashActual(hashService.calcularHashPausa(p, hashAnterior));

        return toResponse(pausaRepo.save(p));
    }

    @Transactional
    public PausaResponse reanudar(Long empleadoId, ReanudarPausaRequest req) {
        Fichaje fichaje = fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay fichaje abierto"));

        Pausa p = pausaRepo.findByFichajeIdAndHoraFinIsNull(fichaje.getId())
                .orElse(null);

        // ── Idempotencia: si no hay activa pero la pausa ya está reanudada, devolver ──
        if (p == null && req != null && req.clientId() != null && !req.clientId().isBlank()) {
            var pausa = pausaRepo.findByFichajeIdAndClientId(fichaje.getId(), req.clientId());
            if (pausa.isPresent() && pausa.get().getHoraFin() != null) return toResponse(pausa.get());
        }

        if (p == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No tienes pausas activas");

        LocalDateTime horaFin = (req != null && req.clientTimestamp() != null
                ? req.clientTimestamp()
                : LocalDateTime.now()).truncatedTo(ChronoUnit.SECONDS);

        p.setHoraFin(horaFin);
        p.setHashActual(hashService.calcularHashPausa(p, p.getHashAnterior()));

        return toResponse(pausaRepo.save(p));
    }

    public List<PausaResponse> pausasDeFichaje(Long fichajeId) {
        return pausaRepo.findByFichajeIdOrderByHoraInicioAsc(fichajeId)
                .stream().map(this::toResponse).toList();
    }

    public PausaResponse pausaActiva(Long empleadoId) {
        Fichaje fichaje = fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId).orElse(null);
        if (fichaje == null) return null;
        return pausaRepo.findByFichajeIdAndHoraFinIsNull(fichaje.getId())
                .map(this::toResponse).orElse(null);
    }

    private PausaResponse toResponse(Pausa p) {
        return new PausaResponse(
                p.getId(),
                p.getFichaje().getId(),
                p.getHoraInicio(),
                p.getHoraFin(),
                p.getTipo(),
                p.isComputa()
        );
    }
}