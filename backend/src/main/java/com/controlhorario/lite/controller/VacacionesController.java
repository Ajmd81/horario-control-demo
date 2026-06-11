package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.VacacionesDtos.*;
import com.controlhorario.lite.service.VacacionesService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vacaciones")
@RequiredArgsConstructor
public class VacacionesController {

    private final VacacionesService service;

    @PostMapping
    public ResponseEntity<VacacionesResponse> solicitar(
            @RequestBody SolicitudRequest req,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.solicitar(empleadoId(auth), req));
    }

    @GetMapping("/mis")
    public ResponseEntity<List<VacacionesResponse>> mis(Authentication auth) {
        return ResponseEntity.ok(service.misSolicitudes(empleadoId(auth)));
    }

    @GetMapping("/empresa")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<VacacionesResponse>> empresa(Authentication auth) {
        return ResponseEntity.ok(service.solicitudesEmpresa(empresaId(auth)));
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<VacacionesResponse> aprobar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.aprobar(id, usuarioId(auth)));
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<VacacionesResponse> rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) RechazoRequest body,
            Authentication auth) {
        String motivo = body != null ? body.motivo() : null;
        return ResponseEntity.ok(service.rechazar(id, usuarioId(auth), motivo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication auth) {
        service.cancelar(id, empleadoId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/disponibles")
    public ResponseEntity<DisponiblesResponse> disponibles(
            Authentication auth,
            @RequestParam(required = false) Integer anio) {
        int year = anio != null ? anio : LocalDate.now().getYear();
        return ResponseEntity.ok(service.calcularDisponibles(empleadoId(auth), year));
    }

    private Long empleadoId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empleadoId", Long.class);
    }

    private Long empresaId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empresaId", Long.class);
    }

    private Long usuarioId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("usuarioId", Long.class);
    }
}