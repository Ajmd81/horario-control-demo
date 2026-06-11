package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.AuditoriaFichaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaFichajeRepository extends JpaRepository<AuditoriaFichaje, Long> {
    List<AuditoriaFichaje> findByFichajeIdOrderByTimestampDesc(Long fichajeId);
}