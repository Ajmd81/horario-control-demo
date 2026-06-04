package com.controlhorario.lite.dto;

public record FichajeEntradaRequest(
        Double latitud,
        Double longitud,
        String deviceId
) {}
