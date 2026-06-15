package com.controlhorario.lite.controller;

import com.controlhorario.lite.service.StripeService;
import com.stripe.exception.StripeException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;

    /** ADMIN crea una sesión de checkout para suscribirse. */
    @PostMapping("/checkout-session")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, String>> crearCheckout(
            @RequestBody Map<String, String> req,
            Authentication auth) throws StripeException {
        Claims c = (Claims) auth.getDetails();
        Long empresaId = c.get("empresaId", Long.class);
        String plan = req.get("plan");
        String url = stripeService.crearCheckoutSession(empresaId, plan);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** ADMIN abre el customer portal para gestionar suscripción. */
    @PostMapping("/customer-portal")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, String>> portal(Authentication auth) throws StripeException {
        Claims c = (Claims) auth.getDetails();
        Long empresaId = c.get("empresaId", Long.class);
        String url = stripeService.crearCustomerPortalSession(empresaId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Endpoint público para webhooks de Stripe. NO requiere autenticación. */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        stripeService.procesarWebhook(payload, sigHeader);
        return ResponseEntity.ok("");
    }
}