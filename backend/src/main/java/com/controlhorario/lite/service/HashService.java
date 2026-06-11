package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.Fichaje;
import com.controlhorario.lite.entity.Pausa;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calcula hashes SHA-256 encadenados.
 * Cada fichaje se enlaza con el hash del anterior del mismo empleado.
 * Si alguien modifica un registro en BBDD, los hashes posteriores no validarán.
 */
@Service
public class HashService {

    /** Hash de un fichaje: SHA-256(hashAnterior + datos del fichaje + version). */
    public String calcularHashFichaje(Fichaje f, String hashAnterior) {
        String payload = (hashAnterior != null ? hashAnterior : "GENESIS") + "|" +
                f.getEmpleado().getId()                         + "|" +
                f.getHoraEntrada()                              + "|" +
                (f.getHoraSalida() != null ? f.getHoraSalida() : "") + "|" +
                (f.getLatitud()    != null ? f.getLatitud()    : "") + "|" +
                (f.getLongitud()   != null ? f.getLongitud()   : "") + "|" +
                f.getTipo()                                     + "|" +
                f.isCerrado()                                   + "|" +
                f.getVersion();
        return sha256(payload);
    }

    /** Hash de una pausa. */
    public String calcularHashPausa(Pausa p, String hashAnterior) {
        String payload = (hashAnterior != null ? hashAnterior : "GENESIS") + "|" +
                p.getFichaje().getId()                          + "|" +
                p.getHoraInicio()                               + "|" +
                (p.getHoraFin() != null ? p.getHoraFin() : "")  + "|" +
                p.getTipo()                                     + "|" +
                p.isComputa();
        return sha256(payload);
    }

    /** Hash de una entrada de auditoría — encadena con el hash del fichaje. */
    public String calcularHashAuditoria(Long fichajeId, String accion, String valorAntes,
                                         String valorDespues, String motivo, Long usuarioId) {
        String payload = fichajeId + "|" + accion + "|" +
                (valorAntes   != null ? valorAntes   : "") + "|" +
                (valorDespues != null ? valorDespues : "") + "|" +
                motivo + "|" +
                (usuarioId != null ? usuarioId : "");
        return sha256(payload);
    }

    /** Hash de la firma de compensación de extras. */
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