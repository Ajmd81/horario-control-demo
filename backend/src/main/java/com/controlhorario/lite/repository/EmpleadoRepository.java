package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Empleado;
import com.controlhorario.lite.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @Query("SELECT e FROM Empleado e JOIN FETCH e.usuario WHERE e.empresa.id = :empresaId AND e.activo = true")
    List<Empleado> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Empleado> findByUsuarioId(Long usuarioId);

    long countByEmpresaIdAndUsuario_RoleAndActivoTrue(Long empresaId, Usuario.Role role);
}
