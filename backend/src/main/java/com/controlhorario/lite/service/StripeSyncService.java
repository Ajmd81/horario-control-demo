package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.Empresa;
import com.controlhorario.lite.repository.EmpresaRepository;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSyncService {

    private final EmpresaRepository empresaRepo;

    /**
     * Sincroniza el estado real de Stripe con la BD para todas las empresas
     * que tienen stripeSubscriptionId. Devuelve un resumen de los cambios.
     *
     * Endpoint one-shot: POST /api/superadmin/stripe/sync
     * Solo ejecutar una vez para reconciliar los eventos perdidos.
     */
    @Transactional
    public SyncResult sincronizarTodasLasSuscripciones() {
        List<Empresa> empresas = empresaRepo.findAllByStripeSubscriptionIdIsNotNull();

        List<String> actualizadas = new ArrayList<>();
        List<String> sinCambios   = new ArrayList<>();
        List<String> errores      = new ArrayList<>();

        for (Empresa empresa : empresas) {
            try {
                Subscription sub = Subscription.retrieve(empresa.getStripeSubscriptionId());

                String nuevoStatus = sub.getStatus().toUpperCase();
                LocalDateTime nuevoPeriodEnd = LocalDateTime.ofEpochSecond(
                        sub.getCurrentPeriodEnd(), 0, ZoneOffset.UTC);

                // Detectar cancel_at_period_end (cancelación programada, aún activa)
                // En Stripe: status=active pero cancelAtPeriodEnd=true
                boolean cancelAtPeriodEnd = Boolean.TRUE.equals(sub.getCancelAtPeriodEnd());
                if (cancelAtPeriodEnd && "ACTIVE".equals(nuevoStatus)) {
                    nuevoStatus = "CANCEL_AT_PERIOD_END";
                }

                String statusAnterior = empresa.getSubscriptionStatus();
                LocalDateTime periodEndAnterior = empresa.getCurrentPeriodEnd();

                boolean cambio = !nuevoStatus.equals(statusAnterior)
                        || !nuevoPeriodEnd.equals(periodEndAnterior);

                if (cambio) {
                    empresa.setSubscriptionStatus(nuevoStatus);
                    empresa.setCurrentPeriodEnd(nuevoPeriodEnd);
                    empresaRepo.save(empresa);

                    String msg = String.format(
                            "%s: %s → %s (period_end: %s)",
                            empresa.getSlug(), statusAnterior, nuevoStatus, nuevoPeriodEnd);
                    actualizadas.add(msg);
                    log.warn("SYNC: {}", msg);
                } else {
                    sinCambios.add(empresa.getSlug() + ": " + nuevoStatus + " (sin cambio)");
                    log.info("SYNC sin cambio: {} → {}", empresa.getSlug(), nuevoStatus);
                }

            } catch (Exception e) {
                String msg = empresa.getSlug() + ": " + e.getMessage();
                errores.add(msg);
                log.error("SYNC error empresa {}: {}", empresa.getSlug(), e.getMessage());
            }
        }

        return new SyncResult(actualizadas, sinCambios, errores);
    }

    public record SyncResult(
            List<String> actualizadas,
            List<String> sinCambios,
            List<String> errores
    ) {
        public boolean tieneErrores() { return !errores.isEmpty(); }
        public int total() { return actualizadas.size() + sinCambios.size() + errores.size(); }
    }
}