package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Fichaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FichajeRepository extends JpaRepository<Fichaje, Long> {

    @Query("SELECT f FROM Fichaje f JOIN FETCH f.empleado WHERE f.empleado.id = :empleadoId ORDER BY f.horaEntrada DESC")
    List<Fichaje> findByEmpleadoId(Long empleadoId);

    @Query("SELECT f FROM Fichaje f JOIN FETCH f.empleado WHERE f.empleado.empresa.id = :empresaId ORDER BY f.horaEntrada DESC")
    List<Fichaje> findByEmpresaId(Long empresaId);

    Optional<Fichaje> findByEmpleadoIdAndCerradoFalse(Long empleadoId);
}
