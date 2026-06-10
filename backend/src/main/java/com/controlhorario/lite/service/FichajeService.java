package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.*;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FichajeService {

    private final FichajeRepository fichajeRepo;
    private final EmpleadoRepository empleadoRepo;
    private final ComputoService computoService;   // ← inyectado

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public FichajeResponse entrada(Long empleadoId, FichajeEntradaRequest req) {
        if (fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes un fichaje abierto. Registra la salida primero.");

        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // ── Bloqueo por límite anual de horas extras (Art. 35.2 ET) ──
        double horasContratadasDia = emp.getHorasContratadasMin() / 60.0;
        double extrasAnio = computoService.calcularExtrasAcumuladasAnio(
                empleadoId, LocalDate.now().getYear(), horasContratadasDia);

        if (extrasAnio >= ComputoService.LIMITE_ANUAL_EXTRAS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "LIMITE_ANUAL_EXTRAS: Has alcanzado las 80 horas extras anuales legales. " +
                    "No se pueden registrar más fichajes este año.");
        }

        Fichaje f = Fichaje.builder()
                .horaEntrada(LocalDateTime.now())
                .latitud(req.latitud())
                .longitud(req.longitud())
                .cerrado(false)
                .empleado(emp)
                .build();
        return toResponse(fichajeRepo.save(f));
    }

    @Transactional
    public FichajeResponse salida(Long empleadoId) {
        Fichaje f = fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay fichaje abierto"));
        f.setHoraSalida(LocalDateTime.now());
        f.setCerrado(true);
        return toResponse(fichajeRepo.save(f));
    }

    public List<FichajeResponse> misFichajes(Long empleadoId) {
        return fichajeRepo.findByEmpleadoId(empleadoId)
                .stream().map(this::toResponse).toList();
    }

    public List<FichajeResponse> todosEnEmpresa(Long empresaId) {
        return fichajeRepo.findByEmpresaId(empresaId)
                .stream().map(this::toResponse).toList();
    }

    public FichajeResponse fichajeActivo(Long empleadoId) {
        return fichajeRepo.findByEmpleadoIdAndCerradoFalse(empleadoId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin fichaje activo"));
    }

    private FichajeResponse toResponse(Fichaje f) {
        return new FichajeResponse(
                f.getId(),
                f.getHoraEntrada() != null ? f.getHoraEntrada().format(FMT) : null,
                f.getHoraSalida() != null ? f.getHoraSalida().format(FMT) : null,
                f.getLatitud(), f.getLongitud(),
                f.isCerrado(),
                f.getEmpleado().getId(),
                f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellido()
        );
    }
}