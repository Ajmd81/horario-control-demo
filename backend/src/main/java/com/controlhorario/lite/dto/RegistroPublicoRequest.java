package com.controlhorario.lite.dto;

public record RegistroPublicoRequest(
    String nombreEmpresa,
    String email,
    String password
) {
}