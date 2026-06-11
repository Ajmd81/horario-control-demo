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

        if (pausaRepo.findByFichajeIdAndHoraFinIsNull(fichaje.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una pausa activa. Reanuda antes de iniciar otra.");

        Pausa.Tipo tipo = req.tipo() != null ? req.tipo() : Pausa.Tipo.DESCANSO;
        boolean computa = tipo == Pausa.Tipo.INTERRUPCION;

        Pausa p = Pausa.builder()
                .fichaje(fichaje)
                .horaInicio(LocalDateTime.now())
                .tipo(tipo)
                .computa(computa)
                .build();

        // Hash chain con la pausa anterior del mismo fichaje
        String hashAnterior = pausaRepo.findByFichajeIdOrderByHoraInicioAsc(fichaje.getId())
                .stream().reduce((a, b) -> b)
                .map(Pausa::getHashActual)
                .orElse(fichaje.getHashActual());
        p.setHashAnterior(hashAnterior);
        p.setHashActual(hashService.calcularHashPausa(p, hashAnterior));

        return toResponse(pausaRepo.save(p));
    }

    @Transactional
    public PausaResponse reanudar(Long empleadoId) {
        Fichaje fichaje = fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay fichaje abierto"));

        Pausa p = pausaRepo.findByFichajeIdAndHoraFinIsNull(fichaje.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No tienes pausas activas"));

        p.setHoraFin(LocalDateTime.now());
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