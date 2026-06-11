package com.controlhorario.lite.controller;

import com.controlhorario.lite.service.ExportacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exportacion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','EMPLOYEE')")
public class ExportacionController {

    private final ExportacionService service;

    @GetMapping("/empleado/{empleadoId}/pdf")
    public ResponseEntity<byte[]> pdf(
            @PathVariable Long empleadoId,
            @RequestParam int anio,
            @RequestParam int mes) {
        byte[] pdf = service.generarPdfEmpleado(empleadoId, anio, mes);
        String filename = "registro-jornada-" + empleadoId + "-" + anio + "-" + String.format("%02d", mes) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/empleado/{empleadoId}/csv")
    public ResponseEntity<byte[]> csv(
            @PathVariable Long empleadoId,
            @RequestParam int anio,
            @RequestParam int mes) {
        byte[] csv = service.generarCsvEmpleado(empleadoId, anio, mes);
        String filename = "registro-jornada-" + empleadoId + "-" + anio + "-" + String.format("%02d", mes) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}