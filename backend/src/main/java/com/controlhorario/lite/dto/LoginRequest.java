package com.controlhorario.lite.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String empresaSlug,
        @NotBlank String username,
        @NotBlank String password,
        String deviceId
) {}
