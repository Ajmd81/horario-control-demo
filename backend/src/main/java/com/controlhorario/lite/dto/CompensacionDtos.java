package com.controlhorario.lite.dto;

import com.controlhorario.lite.entity.CompensacionExtras;
import java.time.LocalDateTime;

public class CompensacionDtos {

    public record FirmarRequest(CompensacionExtras.Modo modo) {}

    public record CompensacionResponse(
        Long id,
        Long empleadoId,
        int anio,
        int mes,
        double horasExtrasDiurnas,
        double horasExtrasNocturnas,
        CompensacionExtras.Modo modoCompensacion,
        boolean firmado,
        LocalDateTime fechaFirma,
        String hashFirma
    ) {}
}