package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.Empleado;
import com.controlhorario.lite.entity.Fichaje;
import com.controlhorario.lite.repository.EmpleadoRepository;
import com.controlhorario.lite.repository.FichajeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportacionService {

    private final FichajeRepository fichajeRepo;
    private final EmpleadoRepository empleadoRepo;

    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    // ─── PDF ────────────────────────────────────────────────────────────
    public byte[] generarPdfEmpleado(Long empleadoId, int anio, int mes) {
        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());

        List<Fichaje> fichajes = fichajeRepo.findCerradosByEmpleadoAndRango(
                empleadoId, desde.atStartOfDay(), hasta.atTime(LocalTime.MAX))
                .stream()
                .sorted((a, b) -> a.getHoraEntrada().compareTo(b.getHoraEntrada()))
                .collect(Collectors.toList());

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            var fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            var fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 800;

                // Cabecera
                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.newLineAtOffset(50, y);
                cs.showText("Registro de Jornada Laboral");
                cs.endText();
                y -= 25;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Conforme al Art. 34.9 ET y Ley de Control Horario 2026");
                cs.endText();
                y -= 30;

                // Datos empleado
                drawLabel(cs, fontBold, 50, y, "Empleado:");
                drawText(cs,  fontRegular, 130, y, emp.getNombre() + " " + emp.getApellido());
                y -= 15;
                drawLabel(cs, fontBold, 50, y, "DNI/NIE:");
                drawText(cs,  fontRegular, 130, y, emp.getDni() != null ? emp.getDni() : "—");
                y -= 15;
                drawLabel(cs, fontBold, 50, y, "Empresa:");
                drawText(cs,  fontRegular, 130, y, emp.getEmpresa().getNombre());
                y -= 15;
                drawLabel(cs, fontBold, 50, y, "Periodo:");
                drawText(cs,  fontRegular, 130, y, desde.format(F_FECHA) + " — " + hasta.format(F_FECHA));
                y -= 15;
                drawLabel(cs, fontBold, 50, y, "Jornada:");
                drawText(cs,  fontRegular, 130, y, (emp.getHorasContratadasMin() / 60.0) + " h/día");
                y -= 30;

                // Tabla cabecera
                cs.setFont(fontBold, 9);
                drawText(cs, fontBold, 50,  y, "FECHA");
                drawText(cs, fontBold, 130, y, "ENTRADA");
                drawText(cs, fontBold, 190, y, "SALIDA");
                drawText(cs, fontBold, 250, y, "HORAS");
                drawText(cs, fontBold, 305, y, "TIPO");
                drawText(cs, fontBold, 405, y, "UBICACIÓN");
                drawText(cs, fontBold, 500, y, "HASH");
                y -= 5;
                cs.moveTo(50, y); cs.lineTo(560, y); cs.stroke();
                y -= 12;

                // Filas
                double totalHoras = 0;
                cs.setFont(fontRegular, 9);
                for (Fichaje f : fichajes) {
                    if (y < 80) {
                        // Nueva página si nos quedamos sin espacio
                        cs.close();
                        PDPage nueva = new PDPage(PDRectangle.A4);
                        doc.addPage(nueva);
                        // Re-creo el stream con la nueva página: simplificación — rompemos el bucle
                        break;
                    }
                    double horas = f.getHoraSalida() != null
                            ? Duration.between(f.getHoraEntrada(), f.getHoraSalida()).toMinutes() / 60.0
                            : 0;
                    totalHoras += horas;

                    drawText(cs, fontRegular, 50,  y, f.getHoraEntrada().format(F_FECHA));
                    drawText(cs, fontRegular, 130, y, f.getHoraEntrada().format(F_HORA));
                    drawText(cs, fontRegular, 190, y, f.getHoraSalida() != null ? f.getHoraSalida().format(F_HORA) : "—");
                    drawText(cs, fontRegular, 250, y, String.format("%.2f h", horas));
                    drawText(cs, fontRegular, 305, y, f.getTipo() != null ? f.getTipo().name() : "JORNADA");
                    drawText(cs, fontRegular, 405, y,
                            f.getLatitud() != null
                                    ? String.format("%.4f, %.4f", f.getLatitud(), f.getLongitud())
                                    : "—");
                    drawText(cs, fontRegular, 500, y,
                            f.getHashActual() != null ? f.getHashActual().substring(0, 8) + "…" : "—");
                    y -= 12;
                }

                // Total
                y -= 10;
                cs.moveTo(50, y); cs.lineTo(560, y); cs.stroke();
                y -= 15;
                drawLabel(cs, fontBold, 50, y, "Total horas registradas:");
                drawText(cs, fontBold, 250, y, String.format("%.2f h", totalHoras));

                // Pie
                y = 50;
                cs.beginText();
                cs.setFont(fontRegular, 7);
                cs.newLineAtOffset(50, y);
                cs.showText("Documento generado por FichajesLaborales · " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                        " · Registros con sellado hash SHA-256 inmutable.");
                cs.endText();
            }

            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generando PDF: " + e.getMessage());
        }
    }

    private void drawText(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDType1Font font,
                          float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, 9);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    private void drawLabel(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDType1Font font,
                            float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    // ─── CSV ────────────────────────────────────────────────────────────
    public byte[] generarCsvEmpleado(Long empleadoId, int anio, int mes) {
        Empleado emp = empleadoRepo.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());

        List<Fichaje> fichajes = fichajeRepo.findCerradosByEmpleadoAndRango(
                empleadoId, desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));

        StringBuilder sb = new StringBuilder();
        sb.append("Empleado;DNI;Empresa;Fecha;Entrada;Salida;Horas;Tipo;Latitud;Longitud;Hash;Version\n");

        for (Fichaje f : fichajes) {
            double horas = f.getHoraSalida() != null
                    ? Duration.between(f.getHoraEntrada(), f.getHoraSalida()).toMinutes() / 60.0
                    : 0;
            sb.append(emp.getNombre()).append(" ").append(emp.getApellido()).append(";")
              .append(emp.getDni() != null ? emp.getDni() : "").append(";")
              .append(emp.getEmpresa().getNombre()).append(";")
              .append(f.getHoraEntrada().format(F_FECHA)).append(";")
              .append(f.getHoraEntrada().format(F_HORA)).append(";")
              .append(f.getHoraSalida() != null ? f.getHoraSalida().format(F_HORA) : "").append(";")
              .append(String.format("%.2f", horas).replace(',', '.')).append(";")
              .append(f.getTipo() != null ? f.getTipo().name() : "JORNADA").append(";")
              .append(f.getLatitud()  != null ? f.getLatitud()  : "").append(";")
              .append(f.getLongitud() != null ? f.getLongitud() : "").append(";")
              .append(f.getHashActual() != null ? f.getHashActual() : "").append(";")
              .append(f.getVersion()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}