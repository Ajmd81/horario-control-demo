package com.controlhorario.lite.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RltDtos {

    /** Resumen global de la empresa para RLT. */
    public record ResumenEmpresa(
        String empresaNombre,
        int    anio,
        int    mes,
        int    empleadosTotales,
        int    empleadosActivos,
        double horasOrdinariasTotal,
        double horasExtrasDiurnasTotal,
        double horasExtrasNocturnasTotal,
        double mediaHorasPorEmpleado,
        int    empleadosAlLimiteExtras,    // empleados con 80h+ acumuladas en el año
        int    fichajesModificados,        // fichajes con version > 1 en el periodo
        int    vacacionesAprobadasMes,
        int    vacacionesPendientes,
        int    vacacionesRechazadas
    ) {}

    /** Empleado anonimizado para RLT. */
    public record EmpleadoAnonimo(
        String codigoAnonimo,        // "EMP-007"
        double horasContratadasDia,
        double horasOrdinarias,
        double horasExtrasDiurnas,
        double horasExtrasNocturnas,
        double totalHoras,
        double extrasAcumuladasAnio,
        boolean limiteAnualSuperado
    ) {}

    /** Fichaje anonimizado para RLT. */
    public record FichajeAnonimo(
        Long          id,
        String        codigoAnonimo,
        LocalDateTime horaEntrada,
        LocalDateTime horaSalida,
        String        tipo,
        boolean       cerrado,
        int           version,                  // si es > 1, fue modificado
        boolean       modificado,
        boolean       conGeolocalizacion        // solo si tiene ubicación, sin lat/lng
    ) {}

    /** Vacaciones anonimizadas para RLT. */
    public record VacacionesAnonima(
        Long      id,
        String    codigoAnonimo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int       diasLaborables,
        String    estado,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaResolucion
    ) {}

    /** Lista paginada de resultados para no devolver miles de filas. */
    public record PaginaFichajes(
        List<FichajeAnonimo> items,
        int total
    ) {}
}