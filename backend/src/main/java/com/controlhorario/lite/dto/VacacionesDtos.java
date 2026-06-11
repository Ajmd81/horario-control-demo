package com.controlhorario.lite.dto;

import com.controlhorario.lite.entity.Vacaciones;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class VacacionesDtos {

    public record SolicitudRequest(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String comentario
    ) {}

    public record RechazoRequest(String motivo) {}

    public record VacacionesResponse(
        Long id,
        Long empleadoId,
        String empleadoNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int diasLaborables,
        Vacaciones.Estado estado,
        String comentario,
        String motivoRechazo,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaResolucion,
        String resueltoPor
    ) {}

    public record DisponiblesResponse(
        int anio,
        int diasTotales,
        int diasUsados,
        int diasPendientes,
        int diasDisponibles
    ) {}
}