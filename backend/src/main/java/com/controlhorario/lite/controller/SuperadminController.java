package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.CrearEmpresaDemoRequest;
import com.controlhorario.lite.repository.EmpresaRepository;
import com.controlhorario.lite.service.StripeSyncService;
import com.controlhorario.lite.service.SuperadminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")   // fix: era 'SUPERADMIN', no cuadraba con el enum
@RequiredArgsConstructor
@Slf4j                                     // fix: faltaba para que log.warn compile
public class SuperadminController {

    private final SuperadminService superadminService;
    private final EmpresaRepository empresaRepo;
    private final StripeSyncService stripeSyncService;  // fix: no estaba inyectado

    @PostMapping("/empresas/demo")
    public ResponseEntity<?> crearDemo(@Valid @RequestBody CrearEmpresaDemoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superadminService.crearEmpresaDemo(req));
    }

    @GetMapping("/empresas")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(empresaRepo.findAll());
    }

    @PatchMapping("/empresas/{id}/activar-licencia")
    public ResponseEntity<?> activar(@PathVariable Long id) {
        superadminService.activarLicencia(id);
        return ResponseEntity.ok(Map.of("message", "Licencia activada"));
    }

    /** Sincroniza el estado real de Stripe con la BD para todas las empresas con suscripción.
     *  One-shot: ejecutar una vez para reconciliar los eventos perdidos mientras el webhook estuvo roto. */
    @PostMapping("/stripe/sync")
    public ResponseEntity<Map<String, Object>> sincronizarStripe() {
        log.warn("SUPER_ADMIN ejecutando sync manual de suscripciones Stripe");

        StripeSyncService.SyncResult result = stripeSyncService.sincronizarTodasLasSuscripciones();

        Map<String, Object> response = Map.of(
                "total",        result.total(),
                "actualizadas", result.actualizadas(),
                "sinCambios",   result.sinCambios(),
                "errores",      result.errores(),
                "ok",           !result.tieneErrores()
        );

        HttpStatus status = result.tieneErrores() ? HttpStatus.MULTI_STATUS : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}