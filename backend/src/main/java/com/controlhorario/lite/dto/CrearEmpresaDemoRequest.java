package com.controlhorario.lite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearEmpresaDemoRequest(
        @NotBlank String nombreEmpresa,
        @NotBlank @Size(min = 3, max = 50) String slug,
        @NotBlank String adminUsername,
        @NotBlank @Size(min = 6) String adminPassword,
        Integer diasDemo,
        Integer maxEmpleados
) {}
