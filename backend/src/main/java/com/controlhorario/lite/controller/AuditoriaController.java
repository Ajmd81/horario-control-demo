package com.controlhorario.lite.controller;

import com.controlhorario.lite.entity.AuditoriaFichaje;
import com.controlhorario.lite.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AuditoriaController {

    private final AuditoriaService service;

    @GetMapping("/fichaje/{fichajeId}")
    public ResponseEntity<List<AuditoriaItem>> historial(@PathVariable Long fichajeId) {
        List<AuditoriaItem> items = service.historial(fichajeId).stream()
                .map(this::toItem).toList();
        return ResponseEntity.ok(items);
    }

    private AuditoriaItem toItem(AuditoriaFichaje a) {
        return new AuditoriaItem(
                a.getId(),
                a.getFichajeId(),
                a.getAccion().name(),
                a.getValorAntes(),
                a.getValorDespues(),
                a.getMotivo(),
                a.getModificadoPor() != null ? a.getModificadoPor().getUsername() : "SISTEMA",
                a.getTimestamp(),
                a.getHash()
        );
    }

    public record AuditoriaItem(
        Long id, Long fichajeId, String accion,
        String valorAntes, String valorDespues,
        String motivo, String modificadoPor,
        LocalDateTime timestamp, String hash
    ) {}
}