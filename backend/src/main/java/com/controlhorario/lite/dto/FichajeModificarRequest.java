package com.controlhorario.lite.dto;

import com.controlhorario.lite.entity.Fichaje;
import java.time.LocalDateTime;

public record FichajeModificarRequest(
    LocalDateTime horaEntrada,
    LocalDateTime horaSalida,
    Fichaje.Tipo tipo,
    String observaciones,
    String motivo
) {}