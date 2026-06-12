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

        Usuario.Role role = Usuario.Role.valueOf(req.role());

        // ── Límite demo ───────────────────────────────────────────
        if (empresa.isDemo() && role == Usuario.Role.EMPLOYEE) {
                int max = empresa.getMaxEmpleadosDemo() != null ? empresa.getMaxEmpleadosDemo() : 3;
                long actual = empleadoRepo.countByEmpresaIdAndUsuario_RoleAndActivoTrue(
                        empresaId, Usuario.Role.EMPLOYEE);
                if (actual >= max) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "DEMO_LIMITE_EMPLEADOS: Límite de " + max +
                        " trabajadores alcanzado en el plan demo.");
                }
        }

        // ── Username único en empresa ─────────────────────────────
        if (usuarioRepo.existsByUsernameAndEmpresaId(req.username(), empresaId))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El username ya existe en esta empresa");

        // ── Validar teléfono (obligatorio para empleados) ─────────
        String telefonoNormalizado = null;
        if (req.telefono() != null && !req.telefono().isBlank()) {
                telefonoNormalizado = normalizarTelefonoEs(req.telefono());
                if (!telefonoNormalizado.matches("^[67]\\d{8}$"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Teléfono inválido. Debe ser un móvil español (9 dígitos comenzando por 6 o 7)");
                if (usuarioRepo.findByTelefonoAndEmpresaId(telefonoNormalizado, empresaId).isPresent())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El teléfono ya está registrado en esta empresa");
        } else if (role == Usuario.Role.EMPLOYEE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El teléfono es obligatorio para los empleados");
        }

        // ── Crear usuario ─────────────────────────────────────────
        Usuario usuario = Usuario.builder()
                .username(req.username())
                .password(encoder.encode(req.password()))
                .role(role)
                .telefono(telefonoNormalizado)
                .activo(true)
                .empresa(empresa)
                .build();
        usuarioRepo.save(usuario);

        // ── Crear empleado ────────────────────────────────────────
        Empleado empleado = Empleado.builder()
                .nombre(req.nombre())
                .apellido(req.apellido())
                .dni(req.dni())
                .telefono(telefonoNormalizado)
                .activo(true)
                .usuario(usuario)
                .empresa(empresa)
                .build();
        empleadoRepo.save(empleado);

        return toResponse(empleado);
        }

        /**
         * Normaliza un teléfono español: quita prefijo +34, espacios y guiones.
         */
        private String normalizarTelefonoEs(String input) {
        if (input == null) return null;
        String clean = input.replaceAll("[\\s\\-()]", "");
        if (clean.startsWith("+34"))   clean = clean.substring(3);
        if (clean.startsWith("0034")) clean = clean.substring(4);
        return clean;
        }

    public List<EmpleadoResponse> listar(Long empresaId) {
        return empleadoRepo.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .filter(e -> e.getUsuario() == null || e.getUsuario().getRole() != Usuario.Role.ADMIN)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
        public EmpleadoResponse actualizarJornada(Long empleadoId, Integer horasContratadasMin) {
        if (horasContratadasMin == null || horasContratadasMin < 60 || horasContratadasMin > 720)
                throw new IllegalArgumentException("Jornada inválida (60–720 minutos por día)");

        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));
        emp.setHorasContratadasMin(horasContratadasMin);
        return toResponse(empleadoRepo.save(emp));
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

