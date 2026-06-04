package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameAndEmpresaId(String username, Long empresaId);
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsernameAndEmpresaId(String username, Long empresaId);
}
