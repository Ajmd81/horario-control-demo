package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.PausaDtos.*;
import com.controlhorario.lite.service.PausaService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pausas")
@RequiredArgsConstructor
public class PausaController {

    private final PausaService service;

    @PostMapping("/iniciar")
    public ResponseEntity<PausaResponse> iniciar(
            @RequestBody(required = false) IniciarPausaRequest req,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.iniciar(empleadoId(auth),
                        req != null ? req : new IniciarPausaRequest(null)));
    }

    @PostMapping("/reanudar")
    public ResponseEntity<PausaResponse> reanudar(Authentication auth) {
        return ResponseEntity.ok(service.reanudar(empleadoId(auth)));
    }

    @GetMapping("/activa")
    public ResponseEntity<PausaResponse> activa(Authentication auth) {
        PausaResponse p = service.pausaActiva(empleadoId(auth));
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.noContent().build();
    }

    @GetMapping("/fichaje/{fichajeId}")
    public ResponseEntity<List<PausaResponse>> deFichaje(@PathVariable Long fichajeId) {
        return ResponseEntity.ok(service.pausasDeFichaje(fichajeId));
    }

    private Long empleadoId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empleadoId", Long.class);
    }
}