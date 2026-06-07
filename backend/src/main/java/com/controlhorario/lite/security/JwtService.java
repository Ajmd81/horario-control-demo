package com.controlhorario.lite.security;

import com.controlhorario.lite.entity.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Token sin empleadoId (superadmin, admin) */
    public String generateToken(Usuario usuario) {
        return generateToken(usuario, null);
    }

    /** Token con empleadoId (empleados que fichan) */
    public String generateToken(Usuario usuario, Long empleadoId) {
        var builder = Jwts.builder()
                .subject(usuario.getUsername())
                .claim("role", usuario.getRole().name())
                .claim("empresaId", usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null)
                .claim("usuarioId", usuario.getId());

        if (empleadoId != null) {
            builder.claim("empleadoId", empleadoId);
        }

        return builder
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }
}