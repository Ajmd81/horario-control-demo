package com.controlhorario.lite.dto;

import com.controlhorario.lite.entity.Pausa;
import java.time.LocalDateTime;

public class PausaDtos {

    public record IniciarPausaRequest(
        Pausa.Tipo tipo,
        String clientId,
        LocalDateTime clientTimestamp
    ) {}

    public record ReanudarPausaRequest(
        String clientId,
        LocalDateTime clientTimestamp
    ) {}

    public record PausaResponse(
        Long id,
        Long fichajeId,
        LocalDateTime horaInicio,
        LocalDateTime horaFin,
        Pausa.Tipo tipo,
        boolean computa
    ) {}
}