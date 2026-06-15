package com.controlhorario.lite.service;

import com.controlhorario.lite.entity.Empresa;
import com.controlhorario.lite.repository.EmpresaRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.price.basico}")
    private String priceBasico;

    @Value("${stripe.price.profesional}")
    private String priceProfesional;

    @Value("${stripe.price.ultimate}")
    private String priceUltimate;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private final EmpresaRepository empresaRepo;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
            log.info("Stripe inicializado en modo {}",
                    stripeSecretKey.startsWith("sk_live") ? "LIVE" : "TEST");
        } else {
            log.warn("Stripe NO inicializado: STRIPE_SECRET_KEY vacía");
        }
    }

    /** Crea una sesión de Checkout para suscribir a un plan. */
    @Transactional
    public String crearCheckoutSession(Long empresaId, String plan) throws StripeException {
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String priceId = switch (plan.toUpperCase()) {
            case "BASICO"      -> priceBasico;
            case "PROFESIONAL" -> priceProfesional;
            case "ULTIMATE"    -> priceUltimate;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Plan inválido: usa BASICO, PROFESIONAL o ULTIMATE");
        };

        // Obtener o crear customer en Stripe
        String customerId = empresa.getStripeCustomerId();
        if (customerId == null) {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(empresa.getNombre())
                    .putMetadata("empresaId", String.valueOf(empresaId))
                    .putMetadata("slug", empresa.getSlug())
                    .build();
            Customer customer = Customer.create(customerParams);
            customerId = customer.getId();
            empresa.setStripeCustomerId(customerId);
            empresaRepo.save(empresa);
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(appBaseUrl + "/" + empresa.getSlug() + "/licencia?success=true&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appBaseUrl + "/" + empresa.getSlug() + "/licencia?canceled=true")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("empresaId", String.valueOf(empresaId))
                .putMetadata("plan", plan.toUpperCase())
                .build();

        Session session = Session.create(params);
        log.info("Checkout session creada para empresa {} plan {}", empresa.getSlug(), plan);
        return session.getUrl();
    }

    /** Crea una sesión del Customer Portal para gestionar la suscripción. */
    public String crearCustomerPortalSession(Long empresaId) throws StripeException {
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (empresa.getStripeCustomerId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta empresa no tiene suscripción activa todavía");

        var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(empresa.getStripeCustomerId())
                .setReturnUrl(appBaseUrl + "/" + empresa.getSlug() + "/licencia")
                .build();

        var session = com.stripe.model.billingportal.Session.create(params);
        return session.getUrl();
    }

    /** Procesa eventos webhook de Stripe. */
    @Transactional
    public void procesarWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }

        log.info("Webhook recibido: {}", event.getType());

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutCompleted(event);
                    break;
                case "customer.subscription.updated":
                case "customer.subscription.deleted":
                    handleSubscriptionChange(event);
                    break;
                case "invoice.payment_failed":
                    log.warn("Pago fallido en evento {}", event.getId());
                    break;
                default:
                    log.debug("Webhook event ignorado: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error procesando webhook {}", event.getType(), e);
        }
    }

    private void handleCheckoutCompleted(Event event) throws StripeException {
        Session session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();

        String empresaIdStr = session.getMetadata().get("empresaId");
        String plan = session.getMetadata().get("plan");
        if (empresaIdStr == null) {
            log.warn("Checkout session sin empresaId en metadata");
            return;
        }

        Long empresaId = Long.parseLong(empresaIdStr);
        Empresa empresa = empresaRepo.findById(empresaId).orElse(null);
        if (empresa == null) return;

        empresa.setStripeSubscriptionId(session.getSubscription());
        empresa.setPlan(plan);
        empresa.setSubscriptionStatus("ACTIVE");
        empresa.setDemo(false);

        if (session.getSubscription() != null) {
            Subscription sub = Subscription.retrieve(session.getSubscription());
            empresa.setCurrentPeriodEnd(
                    LocalDateTime.ofEpochSecond(sub.getCurrentPeriodEnd(), 0, ZoneOffset.UTC)
            );
        }
        empresaRepo.save(empresa);
        log.info("✓ Suscripción activada: empresa {} plan {}", empresa.getSlug(), plan);
    }

    private void handleSubscriptionChange(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();

        Empresa empresa = empresaRepo.findByStripeCustomerId(subscription.getCustomer())
                .orElse(null);
        if (empresa == null) return;

        empresa.setSubscriptionStatus(subscription.getStatus().toUpperCase());
        empresa.setCurrentPeriodEnd(
                LocalDateTime.ofEpochSecond(subscription.getCurrentPeriodEnd(), 0, ZoneOffset.UTC)
        );

        if ("CANCELED".equalsIgnoreCase(subscription.getStatus())) {
            log.warn("Suscripción cancelada para empresa {}", empresa.getSlug());
        }

        empresaRepo.save(empresa);
        log.info("Estado suscripción actualizado: empresa {} → {}", empresa.getSlug(), subscription.getStatus());
    }
}