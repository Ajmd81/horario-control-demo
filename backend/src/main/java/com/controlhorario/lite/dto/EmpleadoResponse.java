package com.controlhorario.lite.dto;

public record EmpleadoResponse(
        Long   id,
        String nombre,
        String apellido,
        String dni,
        String telefono,
        String username,
        String role,
        boolean activo,
        boolean dispositivoVinculado
) {}
