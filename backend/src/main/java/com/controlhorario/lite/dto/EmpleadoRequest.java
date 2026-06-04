package com.controlhorario.lite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpleadoRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        String dni,
        String telefono,
        @NotBlank String username,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String role
) {}
