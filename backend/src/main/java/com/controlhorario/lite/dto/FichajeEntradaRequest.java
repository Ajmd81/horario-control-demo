package com.controlhorario.lite.dto;

import com.controlhorario.lite.entity.Fichaje;
import java.time.LocalDateTime;

public record FichajeEntradaRequest(
    Double latitud,
    Double longitud,
    Fichaje.Tipo tipo,
    String observaciones,
    Boolean mocked,
    String clientId,
    LocalDateTime clientTimestamp
) {}