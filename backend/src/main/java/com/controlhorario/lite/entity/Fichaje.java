package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Fichaje {

    public enum Tipo { JORNADA, DESPLAZAMIENTO, VISITA_COMERCIAL, REUNION_EXTERNA, FORMACION }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id, length = 64")
    private String clientId;

    @Column(name = "hora_entrada", nullable = false)
    private LocalDateTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalDateTime horaSalida;

    private Double latitud;
    private Double longitud;

    @Builder.Default
    @Column(nullable = false)
    private boolean cerrado = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean mocked = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private Tipo tipo = Tipo.JORNADA;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "hash_actual", length = 64)
    private String hashActual;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Builder.Default
    @Column(nullable = false)
    private int version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @OneToMany(mappedBy = "fichaje", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Pausa> pausas = new ArrayList<>();
}