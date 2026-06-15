package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "empresas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Empresa {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String slug;

    @Builder.Default
    @Column(nullable = false)
    private boolean demo = false;

    @Column(name = "fecha_inicio_demo")
    private LocalDate fechaInicioDemo;

    @Builder.Default
    @Column(name = "dias_demo")
    private Integer diasDemo = 15;

    @Builder.Default
    @Column(name = "max_empleados_demo")
    private Integer maxEmpleadosDemo = 3;

    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 64)
    private String stripeSubscriptionId;

    @Column(length = 20)
    private String plan; // BASICO, PROFESIONAL, ULTIMATE

    @Column(name = "subscription_status", length = 20)
    private String subscriptionStatus; // ACTIVE, CANCELED, PAST_DUE, etc.

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    public boolean isDemoExpirada() {
        if (!demo || fechaInicioDemo == null) return false;
        return LocalDate.now().isAfter(fechaInicioDemo.plusDays(diasDemo));
    }

    public long diasRestantesDemo() {
        if (!demo || fechaInicioDemo == null) return -1;
        return Math.max(0, ChronoUnit.DAYS.between(
                LocalDate.now(), fechaInicioDemo.plusDays(diasDemo)));
    }
}
