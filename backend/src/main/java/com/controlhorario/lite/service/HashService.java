package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.Fichaje;
import com.controlhorario.lite.entity.Pausa;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class HashService {

    /** Formatea un LocalDateTime de forma estable (sin nanosegundos). */
    private String fmt(LocalDateTime ldt) {
        return ldt != null ? ldt.truncatedTo(ChronoUnit.SECONDS).toString() : "";
    }

    public String calcularHashFichaje(Fichaje f, String hashAnterior) {
        String payload = (hashAnterior != null ? hashAnterior : "GENESIS") + "|" +
                f.getEmpleado().getId()                         + "|" +
                fmt(f.getHoraEntrada())                         + "|" +
                fmt(f.getHoraSalida())                          + "|" +
                (f.getLatitud()  != null ? f.getLatitud()  : "") + "|" +
                (f.getLongitud() != null ? f.getLongitud() : "") + "|" +
                f.getTipo()                                     + "|" +
                f.isCerrado()                                   + "|" +
                f.isMocked()                                    + "|" +
                f.getVersion();
        return sha256(payload);
    }

    public String calcularHashPausa(Pausa p, String hashAnterior) {
        String payload = (hashAnterior != null ? hashAnterior : "GENESIS") + "|" +
                p.getFichaje().getId()                          + "|" +
                fmt(p.getHoraInicio())                          + "|" +
                fmt(p.getHoraFin())                             + "|" +
                p.getTipo()                                     + "|" +
                p.isComputa();
        return sha256(payload);
    }

    public String calcularHashAuditoria(Long fichajeId, String accion, String valorAntes,
                                         String valorDespues, String motivo, Long usuarioId) {
        String payload = fichajeId + "|" + accion + "|" +
                (valorAntes   != null ? valorAntes   : "") + "|" +
                (valorDespues != null ? valorDespues : "") + "|" +
                motivo + "|" +
                (usuarioId != null ? usuarioId : "");
        return sha256(payload);
    }

    public String calcularHashFirma(Long empleadoId, int anio, int mes, String modo) {
        String payload = empleadoId + "|" + anio + "|" + mes + "|" + modo + "|" +
                System.currentTimeMillis();
        return sha256(payload);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}