package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.RegistroPublicoRequest;
import com.controlhorario.lite.entity.Empleado;
import com.controlhorario.lite.entity.Empresa;
import com.controlhorario.lite.entity.Usuario;
import com.controlhorario.lite.repository.EmpleadoRepository;
import com.controlhorario.lite.repository.EmpresaRepository;
import com.controlhorario.lite.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RegistroPublicoService {

    private final EmpresaRepository empresaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> registrar(RegistroPublicoRequest req) {

        // Validaciones
        if (req.nombreEmpresa() == null || req.nombreEmpresa().isBlank())
            throw new IllegalArgumentException("El nombre de empresa es obligatorio");
        if (req.email() == null || !req.email().contains("@"))
            throw new IllegalArgumentException("Email no válido");
        if (req.password() == null || req.password().length() < 8)
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        if (usuarioRepository.existsByUsername(req.email()))
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");

        // Slug único
        String base = generarSlug(req.nombreEmpresa());
        String slug = base;
        int i = 1;
        while (empresaRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }

        // 1. Empresa en modo demo 15 días
        Empresa empresa = Empresa.builder()
            .nombre(req.nombreEmpresa())
            .slug(slug)
            .demo(true)
            .fechaInicioDemo(LocalDate.now())
            .diasDemo(15)
            .maxEmpleadosDemo(999) // sin límite durante el trial público
            .build();
        empresa = empresaRepository.save(empresa);

        // 2. Usuario ADMIN (primero, porque Empleado tiene FK usuario_id)
        Usuario usuario = Usuario.builder()
            .username(req.email())
            .password(passwordEncoder.encode(req.password()))
            .role(Usuario.Role.ADMIN)
            .empresa(empresa)
            .activo(true)
            .build();
        usuario = usuarioRepository.save(usuario);

        // 3. Empleado vinculado al usuario
        Empleado empleado = Empleado.builder()
            .nombre(extraerNombre(req.email()))
            .apellido("")
            .dni("TEMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .empresa(empresa)
            .usuario(usuario)
            .activo(true)
            .build();
        empleadoRepository.save(empleado);

        return Map.of(
            "message", "Empresa creada correctamente",
            "slug", slug,
            "diasDemo", 15,
            "expira", empresa.getFechaInicioDemo().plusDays(15).toString()
        );
    }

    private String generarSlug(String nombre) {
        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
            .matcher(normalizado)
            .replaceAll("")
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }

    private String extraerNombre(String email) {
        return email.substring(0, email.indexOf("@"));
    }
}