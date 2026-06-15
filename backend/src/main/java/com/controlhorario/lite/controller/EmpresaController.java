package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.EmpresaInfoResponse;
import com.controlhorario.lite.entity.Empresa;
import com.controlhorario.lite.repository.EmpresaRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepo;

    /** Devuelve la información de la empresa del usuario autenticado, incluyendo estado de suscripción. */
    @GetMapping("/mi-empresa")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','EMPLOYEE','RLT')")
    public ResponseEntity<EmpresaInfoResponse> miEmpresa(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        Long empresaId = c.get("empresaId", Long.class);

        Empresa e = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        return ResponseEntity.ok(new EmpresaInfoResponse(
                e.getNombre(),
                e.getSlug(),
                e.isDemo(),
                e.diasRestantesDemo(),
                e.getPlan(),
                e.getSubscriptionStatus(),
                e.getCurrentPeriodEnd()
        ));
    }
}