package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.ComputoDtos.*;
import com.controlhorario.lite.service.ComputoService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/computo")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class ComputoController {

    private final ComputoService computoService;

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<ComputoEmpleadoResponse> computoEmpleado(
            @PathVariable Long empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(computoService.computarEmpleado(empleadoId, desde, hasta));
    }

    @GetMapping("/equipo")
    public ResponseEntity<List<EmpleadoComputoEquipo>> computoEquipo(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        Claims c = (Claims) auth.getDetails();
        Long empresaId = c.get("empresaId", Long.class);
        return ResponseEntity.ok(computoService.computarEquipo(empresaId, desde, hasta));
    }
}