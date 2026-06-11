package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.FichajeEntradaRequest;
import com.controlhorario.lite.dto.FichajeModificarRequest;
import com.controlhorario.lite.dto.FichajeResponse;
import com.controlhorario.lite.service.FichajeService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fichajes")
@RequiredArgsConstructor
public class FichajeController {

    private final FichajeService fichajeService;

    /** El empleado ficha su entrada */
    @PostMapping("/entrada")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> entrada(@RequestBody(required = false) FichajeEntradaRequest req,
                                     Authentication auth) {
        Long empleadoId = empleadoId(auth);
        FichajeEntradaRequest r = req != null
                ? req
                : new FichajeEntradaRequest(null, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fichajeService.entrada(empleadoId, r));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<FichajeResponse> modificar(
            @PathVariable Long id,
            @RequestBody FichajeModificarRequest req,
            Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        Long usuarioId = c.get("usuarioId", Long.class);
        return ResponseEntity.ok(fichajeService.modificar(id, req, usuarioId));
    }

    /** El empleado ficha su salida */
    @PostMapping("/salida")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> salida(Authentication auth) {
        return ResponseEntity.ok(fichajeService.salida(empleadoId(auth)));
    }

    /** El empleado consulta su fichaje activo */
    @GetMapping("/activo")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> activo(Authentication auth) {
        return ResponseEntity.ok(fichajeService.fichajeActivo(empleadoId(auth)));
    }

    /** El empleado ve su historial */
    @GetMapping("/mis-fichajes")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> misFichajes(Authentication auth) {
        return ResponseEntity.ok(fichajeService.misFichajes(empleadoId(auth)));
    }

    /** El admin ve todos los fichajes de la empresa */
    @GetMapping("/equipo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> equipo(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        Long empresaId = c.get("empresaId", Long.class);
        return ResponseEntity.ok(fichajeService.todosEnEmpresa(empresaId));
    }

    private Long empleadoId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empleadoId", Long.class);
    }
}