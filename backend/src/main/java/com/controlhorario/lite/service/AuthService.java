package com.controlhorario.lite.service;

import com.controlhorario.lite.dto.LoginRequest;
import com.controlhorario.lite.dto.LoginResponse;
import com.controlhorario.lite.entity.*;
import com.controlhorario.lite.repository.*;
import com.controlhorario.lite.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmpleadoRepository empleadoRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest req) {

        // ── 1. Login especial superadmin (no tiene empresa) ───────────
        if ("superadmin".equals(req.empresaSlug())) {
            Usuario sa = usuarioRepo.findByUsername(req.username())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

            if (!encoder.matches(req.password(), sa.getPassword()))
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");

            if (!sa.isActivo())
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Cuenta desactivada");

            return LoginResponse.builder()
                    .token(jwtService.generateToken(sa))
                    .username(sa.getUsername())
                    .role(sa.getRole().name())
                    .empresaSlug("superadmin")
                    .empresaNombre("Superadmin")
                    .usuarioId(sa.getId())
                    .build();
        }

        // ── 2. Buscar empresa ──────────────────────────────────────────
        Empresa empresa = empresaRepo.findBySlug(req.empresaSlug())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Empresa no encontrada"));

        // ── 3. Demo expirada ───────────────────────────────────────────
        if (empresa.isDemoExpirada()) {
            log.warn("Demo expirada: {}", empresa.getSlug());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "DEMO_EXPIRADA: El periodo de demo ha finalizado. " +
                    "Contacta con el administrador para activar tu licencia.");
        }

        // ── 4. Credenciales ────────────────────────────────────────────
        Usuario usuario = usuarioRepo
                .findByUsernameAndEmpresaId(req.username(), empresa.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (!encoder.matches(req.password(), usuario.getPassword()))
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");

        if (!usuario.isActivo())
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cuenta desactivada");

        // ── 5. Device binding (solo EMPLOYEE con deviceId) ─────────────
        if (usuario.getRole() == Usuario.Role.EMPLOYEE && req.deviceId() != null) {
            Empleado emp = empleadoRepo.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Empleado no encontrado"));

            if (emp.getDeviceId() == null) {
                emp.setDeviceId(req.deviceId());
                empleadoRepo.save(emp);
                log.info("Device binding creado para {}", usuario.getUsername());
            } else if (!emp.getDeviceId().equals(req.deviceId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "DEVICE_NOT_AUTHORIZED: Este usuario ya está vinculado a otro dispositivo.");
            }
        }

        // ── 6. Token (empleadoId primero, luego el token con ese dato) ──
        Long empleadoId = empleadoRepo.findByUsuarioId(usuario.getId())
                .map(Empleado::getId).orElse(null);

        String token = jwtService.generateToken(usuario, empleadoId);

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .token(token)
                .username(usuario.getUsername())
                .role(usuario.getRole().name())
                .empresaSlug(empresa.getSlug())
                .empresaNombre(empresa.getNombre())
                .usuarioId(usuario.getId())
                .empleadoId(empleadoId);

        if (empresa.isDemo()) {
            builder.demo(true)
                   .diasRestantesDemo(empresa.diasRestantesDemo())
                   .diasTotalesDemo(empresa.getDiasDemo().longValue());
        }

        return builder.build();
    }
}