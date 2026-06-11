package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.CompensacionDtos.*;
import com.controlhorario.lite.entity.CompensacionExtras;
import com.controlhorario.lite.entity.Empleado;
import com.controlhorario.lite.repository.CompensacionExtrasRepository;
import com.controlhorario.lite.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompensacionService {

    private final CompensacionExtrasRepository repo;
    private final EmpleadoRepository empleadoRepo;
    private final ComputoService computoService;
    private final HashService hashService;

    /** Crea/actualiza el registro mensual de extras de un empleado. */
    @Transactional
    public CompensacionResponse generarParaPeriodo(Long empleadoId, int anio, int mes) {
        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());
        var computo = computoService.computarEmpleado(empleadoId, desde, hasta);

        CompensacionExtras c = repo.findByEmpleadoIdAndAnioAndMes(empleadoId, anio, mes)
                .orElseGet(() -> CompensacionExtras.builder()
                        .empleado(emp).anio(anio).mes(mes).build());

        c.setHorasExtrasDiurnas(computo.resumenMensual().horasExtrasDiurnas());
        c.setHorasExtrasNocturnas(computo.resumenMensual().horasExtrasNocturnas());
        return toResponse(repo.save(c));
    }

    public List<CompensacionResponse> pendientes(Long empleadoId) {
        return repo.findByEmpleadoIdAndFirmadoFalse(empleadoId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CompensacionResponse firmar(Long id, Long empleadoId, FirmarRequest req) {
        if (req.modo() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modo de compensación obligatorio");

        CompensacionExtras c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!c.getEmpleado().getId().equals(empleadoId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es tu compensación");
        if (c.isFirmado())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya está firmada");

        c.setModoCompensacion(req.modo());
        c.setFirmado(true);
        c.setFechaFirma(LocalDateTime.now());
        c.setHashFirma(hashService.calcularHashFirma(
                empleadoId, c.getAnio(), c.getMes(), req.modo().name()));

        return toResponse(repo.save(c));
    }

    private CompensacionResponse toResponse(CompensacionExtras c) {
        return new CompensacionResponse(
                c.getId(),
                c.getEmpleado().getId(),
                c.getAnio(), c.getMes(),
                c.getHorasExtrasDiurnas(),
                c.getHorasExtrasNocturnas(),
                c.getModoCompensacion(),
                c.isFirmado(),
                c.getFechaFirma(),
                c.getHashFirma()
        );
    }
}