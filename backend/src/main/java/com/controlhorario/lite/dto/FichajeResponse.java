package com.controlhorario.lite.dto;

public record FichajeResponse(
        Long   id,
        String horaEntrada,
        String horaSalida,
        Double latitud,
        Double longitud,
        boolean cerrado,
        Long   empleadoId,
        String empleadoNombre,
        boolean mocked
) {}
