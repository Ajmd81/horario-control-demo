package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.EmpleadoRequest;
import com.controlhorario.lite.dto.EmpleadoResponse;
import com.controlhorario.lite.service.EmpleadoService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listar(Authentication auth) {
        return ResponseEntity.ok(empleadoService.listar(empresaId(auth)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crear(@Valid @RequestBody EmpleadoRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.crear(req, empresaId(auth)));
    }

    @DeleteMapping("/{id}/dispositivo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetDispositivo(@PathVariable Long id) {
        empleadoService.resetearDispositivo(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/jornada")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmpleadoResponse> actualizarJornada(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer horasContratadasMin = body.get("horasContratadasMin");
        return ResponseEntity.ok(empleadoService.actualizarJornada(id, horasContratadasMin));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        empleadoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    private Long empresaId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empresaId", Long.class);
    }
}
