package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Pausa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PausaRepository extends JpaRepository<Pausa, Long> {
    Optional<Pausa> findByFichajeIdAndHoraFinIsNull(Long fichajeId);
    List<Pausa> findByFichajeIdOrderByHoraInicioAsc(Long fichajeId);
}