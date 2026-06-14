package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.VerificacionIntegridad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VerificacionIntegridadRepository extends JpaRepository<VerificacionIntegridad, Long> {
    List<VerificacionIntegridad> findTop10ByEmpresaIdOrderByFechaDesc(Long empresaId);
}