package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Fichaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FichajeRepository extends JpaRepository<Fichaje, Long> {

    @Query("SELECT f FROM Fichaje f JOIN FETCH f.empleado WHERE f.empleado.id = :empleadoId ORDER BY f.horaEntrada DESC")
    List<Fichaje> findByEmpleadoId(Long empleadoId);

    @Query("SELECT f FROM Fichaje f JOIN FETCH f.empleado WHERE f.empleado.empresa.id = :empresaId ORDER BY f.horaEntrada DESC")
    List<Fichaje> findByEmpresaId(Long empresaId);

    Optional<Fichaje> findByEmpleadoIdAndCerradoFalse(Long empleadoId);

    @Query("""
        SELECT f FROM Fichaje f
        WHERE f.empleado.id = :empleadoId
          AND f.cerrado = true
          AND f.horaEntrada >= :desde
          AND f.horaEntrada <= :hasta
        ORDER BY f.horaEntrada ASC
        """)
    List<Fichaje> findCerradosByEmpleadoAndRango(
        @Param("empleadoId") Long empleadoId,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT f FROM Fichaje f
        WHERE f.empleado.empresa.id = :empresaId
          AND f.cerrado = true
          AND f.horaEntrada >= :desde
          AND f.horaEntrada <= :hasta
        ORDER BY f.empleado.id, f.horaEntrada ASC
        """)
    List<Fichaje> findCerradosByEmpresaAndRango(
        @Param("empresaId") Long empresaId,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );
}