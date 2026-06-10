package com.controlhorario.lite.dto;

import java.time.LocalDate;
import java.util.List;

public class ComputoDtos {

    public record DiaComputo(
        LocalDate fecha,
        double horasOrdinarias,
        double horasExtrasDiurnas,
        double horasExtrasNocturnas,
        double totalHoras
    ) {}

    public record ResumenPeriodo(
        double horasOrdinarias,
        double horasExtrasDiurnas,
        double horasExtrasNocturnas,
        double totalExtras,
        double totalHoras
    ) {}

    public record ComputoEmpleadoResponse(
        Long empleadoId,
        String nombreCompleto,
        double horasContratadasDia,
        double horasExtrasAcumuladasAnio,
        double limiteAnualExtras,
        List<DiaComputo> dias,
        ResumenPeriodo resumenSemanal,
        ResumenPeriodo resumenMensual
    ) {}

    public record EmpleadoComputoEquipo(
        Long empleadoId,
        String nombreCompleto,
        double horasContratadasDia,
        double horasOrdinarias,
        double horasExtrasDiurnas,
        double horasExtrasNocturnas,
        double totalHoras,
        double extrasAcumuladasAnio,
        boolean limiteAnualSuperado
    ) {}
}