package com.controlhorario.lite.dto;

import com.controlhorario.lite.entity.Fichaje;

public record FichajeEntradaRequest(
    Double latitud,
    Double longitud,
    Fichaje.Tipo tipo,
    String observaciones
) {}