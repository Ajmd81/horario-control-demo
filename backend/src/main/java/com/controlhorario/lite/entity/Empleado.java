package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empleados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Empleado {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    private String dni;
    private String telefono;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "device_id")
    private String deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Builder.Default
    @Column(name = "horas_contratadas_min", nullable = false)
    private int horasContratadasMin = 480;  // 8h por defecto
}
