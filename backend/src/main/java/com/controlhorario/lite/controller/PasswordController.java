package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.PasswordDtos.*;
import com.controlhorario.lite.service.PasswordService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService service;

    /** Admin resetea la contraseña de un empleado y recibe la nueva temporal. */
    @PostMapping("/empleados/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ResetPasswordResponse> reset(@PathVariable Long id) {
        String nueva = service.resetearPasswordEmpleado(id);
        return ResponseEntity.ok(new ResetPasswordResponse(nueva));
    }

    /** Usuario autenticado cambia su propia contraseña. */
    @PostMapping("/auth/cambiar-password")
    public ResponseEntity<Void> cambiar(
            @RequestBody CambiarPasswordRequest req,
            Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        Long usuarioId = c.get("usuarioId", Long.class);
        service.cambiarPasswordPropia(usuarioId, req.passwordActual(), req.passwordNueva());
        return ResponseEntity.noContent().build();
    }
}