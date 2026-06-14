package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.IntegridadDtos.*;
import com.controlhorario.lite.service.IntegridadService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/verificacion/integridad")
@RequiredArgsConstructor
public class IntegridadController {

    private final IntegridadService service;

    /** ADMIN: verificación completa con detalle de incidencias. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<IntegridadResultado> verificar(Authentication auth) {
        return ResponseEntity.ok(service.verificarEmpresa(empresaId(auth), false));
    }

    /** ADMIN: histórico de últimas 10 verificaciones. */
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<HistorialItem>> historial(Authentication auth) {
        return ResponseEntity.ok(service.historial(empresaId(auth)));
    }

    /** RLT: resumen anonimizado sin IDs ni nombres. */
    @GetMapping("/resumen")
    @PreAuthorize("hasRole('RLT')")
    public ResponseEntity<IntegridadResumen> resumen(Authentication auth) {
        return ResponseEntity.ok(service.verificarResumen(empresaId(auth)));
    }

    private Long empresaId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empresaId", Long.class);
    }

    /** ADMIN: recalcula los hashes de todos los fichajes. SOLO usar tras cambios en fórmula del hash. */
    @PostMapping("/recalcular")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, Integer>> recalcular(Authentication auth) {
        int n = service.recalcularHashes(empresaId(auth));
        return ResponseEntity.ok(Map.of("recalculados", n));
    }
}