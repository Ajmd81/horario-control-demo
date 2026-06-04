package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.*;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmpresaRepository empresaRepo;
    private final PasswordEncoder encoder;

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest req, Long empresaId) {
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        // ── Límite demo ───────────────────────────────────────────
        if (empresa.isDemo() && Usuario.Role.valueOf(req.role()) == Usuario.Role.EMPLOYEE) {
            int max = empresa.getMaxEmpleadosDemo() != null ? empresa.getMaxEmpleadosDemo() : 3;
            long actual = empleadoRepo.countByEmpresaIdAndUsuario_RoleAndActivoTrue(
                    empresaId, Usuario.Role.EMPLOYEE);
            if (actual >= max) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "DEMO_LIMITE_EMPLEADOS: Límite de " + max +
                        " trabajadores alcanzado en el plan demo.");
            }
        }

        if (usuarioRepo.existsByUsernameAndEmpresaId(req.username(), empresaId))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El username ya existe en esta empresa");

        Usuario usuario = Usuario.builder()
                .username(req.username())
                .password(encoder.encode(req.password()))
                .role(Usuario.Role.valueOf(req.role()))
                .activo(true)
                .empresa(empresa)
                .build();
        usuarioRepo.save(usuario);

        Empleado empleado = Empleado.builder()
                .nombre(req.nombre())
                .apellido(req.apellido())
                .dni(req.dni())
                .telefono(req.telefono())
                .activo(true)
                .usuario(usuario)
                .empresa(empresa)
                .build();
        empleadoRepo.save(empleado);

        return toResponse(empleado);
    }

    public List<EmpleadoResponse> listar(Long empresaId) {
        return empleadoRepo.findByEmpresaIdAndActivoTrue(empresaId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void resetearDispositivo(Long id) {
        Empleado e = empleadoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        e.setDeviceId(null);
        empleadoRepo.save(e);
    }

    @Transactional
    public void desactivar(Long id) {
        Empleado e = empleadoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        e.setActivo(false);
        e.getUsuario().setActivo(false);
        empleadoRepo.save(e);
    }

    private EmpleadoResponse toResponse(Empleado e) {
        return new EmpleadoResponse(
                e.getId(), e.getNombre(), e.getApellido(),
                e.getDni(), e.getTelefono(),
                e.getUsuario().getUsername(),
                e.getUsuario().getRole().name(),
                e.isActivo(),
                e.getDeviceId() != null
        );
    }
}
