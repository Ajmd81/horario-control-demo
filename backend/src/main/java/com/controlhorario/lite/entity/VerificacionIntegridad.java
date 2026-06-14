package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verificaciones_integridad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VerificacionIntegridad {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total_fichajes", nullable = false)
    private int totalFichajes;

    @Column(name = "total_corruptos", nullable = false)
    private int totalCorruptos;

    @Builder.Default
    @Column(nullable = false)
    private boolean automatica = false;
}