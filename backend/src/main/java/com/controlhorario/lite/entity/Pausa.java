package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pausas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pausa {

    public enum Tipo { DESCANSO, COMIDA, INTERRUPCION }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id, length = 64")
    private String clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fichaje_id", nullable = false)
    private Fichaje fichaje;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Tipo tipo = Tipo.DESCANSO;

    @Builder.Default
    @Column(nullable = false)
    private boolean computa = false;

    @Column(name = "hash_actual", length = 64)
    private String hashActual;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;
}