package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fichajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Fichaje {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hora_entrada", nullable = false)
    private LocalDateTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalDateTime horaSalida;

    private Double latitud;
    private Double longitud;

    @Builder.Default
    @Column(nullable = false)
    private boolean cerrado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
}
