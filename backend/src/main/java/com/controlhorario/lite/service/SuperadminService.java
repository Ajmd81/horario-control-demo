package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.CrearEmpresaDemoRequest;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperadminService {

    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;

    @Transactional
    public Map<String, Object> crearEmpresaDemo(CrearEmpresaDemoRequest req) {
        if (empresaRepo.existsBySlug(req.slug()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una empresa con ese slug: " + req.slug());

        int dias = req.diasDemo() != null ? req.diasDemo() : 15;
        int maxEmp = req.maxEmpleados() != null ? req.maxEmpleados() : 3;

        Empresa empresa = Empresa.builder()
                .nombre(req.nombreEmpresa())
                .slug(req.slug())
                .demo(true)
                .fechaInicioDemo(LocalDate.now())
                .diasDemo(dias)
                .maxEmpleadosDemo(maxEmp)
                .build();
        empresaRepo.save(empresa);

        Usuario admin = Usuario.builder()
                .username(req.adminUsername())
                .password(encoder.encode(req.adminPassword()))
                .role(Usuario.Role.ADMIN)
                .activo(true)
                .empresa(empresa)
                .build();
        usuarioRepo.save(admin);

        log.info("✅ Empresa demo creada: {} (slug={})", empresa.getNombre(), empresa.getSlug());

        return Map.of(
                "empresaId", empresa.getId(),
                "nombre", empresa.getNombre(),
                "slug", empresa.getSlug(),
                "adminUsername", admin.getUsername(),
                "demo", true,
                "diasDemo", dias,
                "maxEmpleados", maxEmp,
                "fechaExpiracion", empresa.getFechaInicioDemo().plusDays(dias).toString()
        );
    }

    @Transactional
    public void activarLicencia(Long empresaId) {
        Empresa e = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        e.setDemo(false);
        e.setFechaInicioDemo(null);
        e.setDiasDemo(null);
        e.setMaxEmpleadosDemo(null);
        empresaRepo.save(e);
        log.info("🔓 Licencia completa activada: {}", e.getNombre());
    }
}
