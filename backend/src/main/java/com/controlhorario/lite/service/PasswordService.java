package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.Empleado;
import com.controlhorario.lite.entity.Usuario;
import com.controlhorario.lite.repository.EmpleadoRepository;
import com.controlhorario.lite.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UsuarioRepository  usuarioRepo;
    private final EmpleadoRepository empleadoRepo;
    private final PasswordEncoder    encoder;

    /** Caracteres sin ambigüedades (sin 0/O, 1/l/I). */
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int    LENGTH  = 10;
    private static final SecureRandom RNG = new SecureRandom();

    /** Admin resetea la contraseña de un empleado. Devuelve la nueva en plano (única vez). */
    @Transactional
    public String resetearPasswordEmpleado(Long empleadoId) {
        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

        Usuario u = emp.getUsuario();
        if (u == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El empleado no tiene usuario asociado");

        String nueva = generarPasswordTemporal();
        u.setPassword(encoder.encode(nueva));
        usuarioRepo.save(u);

        return nueva;
    }

    /** Empleado o admin cambia su propia contraseña. */
    @Transactional
    public void cambiarPasswordPropia(Long usuarioId, String passwordActual, String passwordNueva) {
        if (passwordNueva == null || passwordNueva.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe tener al menos 6 caracteres");

        Usuario u = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!encoder.matches(passwordActual, u.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta");

        if (encoder.matches(passwordNueva, u.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña no puede ser igual a la actual");

        u.setPassword(encoder.encode(passwordNueva));
        usuarioRepo.save(u);
    }

    private String generarPasswordTemporal() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARSET.charAt(RNG.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}