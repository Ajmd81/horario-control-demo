package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_fichajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditoriaFichaje {

    public enum Accion { CREATE, UPDATE, DELETE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fichaje_id", nullable = false)
    private Long fichajeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Accion accion;

    @Column(name = "valor_antes", columnDefinition = "TEXT")
    private String valorAntes;

    @Column(name = "valor_despues", columnDefinition = "TEXT")
    private String valorDespues;

    @Column(nullable = false, length = 500)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modificado_por_id")
    private Usuario modificadoPor;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 64)
    private String hash;
}