package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Vacaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VacacionesRepository extends JpaRepository<Vacaciones, Long> {

    @Query("""
        SELECT v FROM Vacaciones v JOIN FETCH v.empleado
        WHERE v.empleado.id = :empleadoId
        ORDER BY v.fechaInicio DESC
        """)
    List<Vacaciones> findByEmpleadoId(@Param("empleadoId") Long empleadoId);

    @Query("""
        SELECT v FROM Vacaciones v JOIN FETCH v.empleado
        WHERE v.empleado.empresa.id = :empresaId
        ORDER BY v.estado ASC, v.fechaInicio DESC
        """)
    List<Vacaciones> findByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("""
        SELECT v FROM Vacaciones v
        WHERE v.empleado.id = :empleadoId
          AND v.estado = com.controlhorario.lite.entity.Vacaciones$Estado.APROBADA
          AND :fecha BETWEEN v.fechaInicio AND v.fechaFin
        """)
    List<Vacaciones> findAprobadasEnFecha(
        @Param("empleadoId") Long empleadoId,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT v FROM Vacaciones v
        WHERE v.empleado.id = :empleadoId
          AND v.estado = com.controlhorario.lite.entity.Vacaciones$Estado.APROBADA
          AND EXTRACT(YEAR FROM v.fechaInicio) = :anio
        """)
    List<Vacaciones> findAprobadasByEmpleadoAndAnio(
        @Param("empleadoId") Long empleadoId,
        @Param("anio") int anio
    );

    @Query("""
        SELECT v FROM Vacaciones v
        WHERE v.empleado.id = :empleadoId
          AND v.estado IN (com.controlhorario.lite.entity.Vacaciones$Estado.PENDIENTE,
                           com.controlhorario.lite.entity.Vacaciones$Estado.APROBADA)
          AND v.fechaInicio <= :fin
          AND v.fechaFin   >= :inicio
        """)
    List<Vacaciones> findSolapadas(
        @Param("empleadoId") Long empleadoId,
        @Param("inicio") LocalDate inicio,
        @Param("fin")    LocalDate fin
    );
}