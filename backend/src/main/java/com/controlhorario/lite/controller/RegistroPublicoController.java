package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.RegistroPublicoRequest;
import com.controlhorario.lite.service.RegistroPublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class RegistroPublicoController {

    private final RegistroPublicoService registroPublicoService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistroPublicoRequest req) {
        try {
            return ResponseEntity.ok(registroPublicoService.registrar(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}