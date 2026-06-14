package com.controlhorario.lite.controller;

import com.controlhorario.lite.dto.RltDtos.*;
import com.controlhorario.lite.service.RltService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rlt")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RLT')")
public class RltController {

    private final RltService service;

    @GetMapping("/resumen")
    public ResponseEntity<ResumenEmpresa> resumen(
            Authentication auth,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        int y = anio != null ? anio : LocalDate.now().getYear();
        int m = mes  != null ? mes  : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(service.resumenEmpresa(empresaId(auth), y, m));
    }

    @GetMapping("/empleados")
    public ResponseEntity<List<EmpleadoAnonimo>> empleados(
            Authentication auth,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        int y = anio != null ? anio : LocalDate.now().getYear();
        int m = mes  != null ? mes  : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(service.empleadosAnonimos(empresaId(auth), y, m));
    }

    @GetMapping("/fichajes")
    public ResponseEntity<List<FichajeAnonimo>> fichajes(
            Authentication auth,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        int y = anio != null ? anio : LocalDate.now().getYear();
        int m = mes  != null ? mes  : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(service.fichajesAnonimos(empresaId(auth), y, m));
    }

    @GetMapping("/vacaciones")
    public ResponseEntity<List<VacacionesAnonima>> vacaciones(Authentication auth) {
        return ResponseEntity.ok(service.vacacionesAnonimas(empresaId(auth)));
    }

    private Long empresaId(Authentication auth) {
        Claims c = (Claims) auth.getDetails();
        return c.get("empresaId", Long.class);
    }
}