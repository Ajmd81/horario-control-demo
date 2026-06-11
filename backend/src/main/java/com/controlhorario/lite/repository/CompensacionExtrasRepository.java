package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.CompensacionExtras;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompensacionExtrasRepository extends JpaRepository<CompensacionExtras, Long> {
    Optional<CompensacionExtras> findByEmpleadoIdAndAnioAndMes(Long empleadoId, int anio, int mes);
    List<CompensacionExtras> findByEmpleadoIdAndFirmadoFalse(Long empleadoId);
}