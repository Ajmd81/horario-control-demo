package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.CrearEmpresaDemoRequest;
import com.controlhorario.lite.repository.EmpresaRepository;
import com.controlhorario.lite.service.SuperadminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class SuperadminController {

    private final SuperadminService superadminService;
    private final EmpresaRepository empresaRepo;

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
}
