package com.controlhorario.lite.dto;

import java.time.LocalDateTime;
import java.util.List;

public class IntegridadDtos {

    public enum TipoIncidencia { HASH_MISMATCH, CADENA_ROTA, MODIFICACION_SIN_AUDIT }

    /** Detalle de un fichaje corrupto. Solo se devuelve a ADMIN. */
    public record FichajeCorrupto(
        Long           fichajeId,
        Long           empleadoId,
        String         empleadoNombre,
        LocalDateTime  horaEntrada,
        int            version,
        TipoIncidencia tipo,
        String         hashEsperado,
        String         hashAlmacenado,
        String         descripcion
    ) {}

    /** Resultado completo para ADMIN. */
    public record IntegridadResultado(
        Long                  empresaId,
        String                empresaNombre,
        LocalDateTime         fecha,
        int                   totalFichajes,
        int                   totalCorruptos,
        boolean               cadenaIntegra,
        List<FichajeCorrupto> incidencias
    ) {}

    /** Resumen anonimizado para RLT (sin IDs ni datos personales). */
    public record IntegridadResumen(
        LocalDateTime fecha,
        int           totalFichajes,
        int           totalCorruptos,
        boolean       cadenaIntegra
    ) {}

    /** Entrada de historial. */
    public record HistorialItem(
        Long          id,
        LocalDateTime fecha,
        int           totalFichajes,
        int           totalCorruptos,
        boolean       cadenaIntegra,
        boolean       automatica
    ) {}
}