package com.controlhorario.lite.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

// ── Auth ──────────────────────────────────────────────────────────────────────

record LoginRequest(
        @NotBlank String empresaSlug,
        @NotBlank String username,
        @NotBlank String password,
        String deviceId  // para device binding en mobile (opcional desde web)
) {}

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class LoginResponse {
    String token;
    String username;
    String role;
    String empresaSlug;
    String empresaNombre;
    Long   usuarioId;
    Long   empleadoId;
    Boolean demo;
    Long    diasRestantesDemo;
}

// ── Empleados ─────────────────────────────────────────────────────────────────

record EmpleadoRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        String dni,
        String telefono,
        @NotBlank String username,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String role   // "ADMIN" | "EMPLOYEE"
) {}

record EmpleadoResponse(
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

// ── Fichajes ──────────────────────────────────────────────────────────────────

record FichajeEntradaRequest(Double latitud, Double longitud, String deviceId) {}

record FichajeResponse(
        Long   id,
        String horaEntrada,
        String horaSalida,
        Double latitud,
        Double longitud,
        boolean cerrado,
        Long   empleadoId,
        String empleadoNombre
) {}

// ── Superadmin ────────────────────────────────────────────────────────────────

record CrearEmpresaDemoRequest(
        @NotBlank String nombreEmpresa,
        @NotBlank @Size(min=3,max=50) String slug,
        @NotBlank String adminUsername,
        @NotBlank @Size(min=6) String adminPassword,
        Integer diasDemo,
        Integer maxEmpleados
) {}
