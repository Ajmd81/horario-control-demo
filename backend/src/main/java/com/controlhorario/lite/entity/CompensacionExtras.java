package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compensaciones_extras",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empleado_id","anio","mes"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompensacionExtras {

    public enum Modo { DINERO, DESCANSO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int mes;

    @Column(name = "horas_extras_diurnas", nullable = false)
    private double horasExtrasDiurnas;

    @Column(name = "horas_extras_nocturnas", nullable = false)
    private double horasExtrasNocturnas;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_compensacion", length = 20)
    private Modo modoCompensacion;

    @Builder.Default
    @Column(nullable = false)
    private boolean firmado = false;

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "hash_firma", length = 64)
    private String hashFirma;
}