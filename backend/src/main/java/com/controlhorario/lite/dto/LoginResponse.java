package com.controlhorario.lite.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String empresaSlug;
    private String empresaNombre;
    private Long   usuarioId;
    private Long   empleadoId;
    private Boolean demo;
    private Long    diasRestantesDemo;
    private Long    diasTotalesDemo;
}