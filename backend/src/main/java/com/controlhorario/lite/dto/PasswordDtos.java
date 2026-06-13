package com.controlhorario.lite.dto;

public class PasswordDtos {

    public record ResetPasswordResponse(String passwordTemporal) {}

    public record CambiarPasswordRequest(String passwordActual, String passwordNueva) {}
}