package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.CompensacionDtos.*;
import com.controlhorario.lite.service.CompensacionService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compensaciones")
@RequiredArgsConstructor
public class CompensacionController {

    private final CompensacionService service;

    /** Admin genera/actualiza el registro mensual de extras de un empleado. */
    @PostMapping("/generar/{empleadoId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<CompensacionResponse> generar(
            @PathVariable Long empleadoId,
            @RequestParam int anio,
            @RequestParam int mes) {
        return ResponseEntity.ok(service.generarParaPeriodo(empleadoId, anio, mes));
    }

    /** Empleado ve sus compensaciones pendientes de firmar. */
    @GetMapping("/pendientes")
    public ResponseEntity<List<CompensacionResponse>> pendientes(Authentication auth) {
        return ResponseEntity.ok(service.pendientes(empleadoId(auth)));
    }

    /** Empleado firma una compensación indicando modo (DINERO o DESCANSO). */
    @PostMapping("/{id}/firmar")
    public ResponseEntity<CompensacionResponse> firmar(
            @PathVariable Long id,
            @RequestBody FirmarRequest req,
            Authentication auth) {
        return ResponseEntity.ok(service.firmar(id, empleadoId(auth), req));
    }

    private Long empleadoId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empleadoId", Long.class);
    }
}