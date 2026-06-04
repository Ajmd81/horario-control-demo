package com.controlhorario.lite.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "empresa_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    public enum Role { SUPERADMIN, ADMIN, EMPLOYEE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;
}
