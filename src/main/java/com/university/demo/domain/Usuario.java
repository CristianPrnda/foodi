package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Usuario de la aplicación (diagrama: Seguridad y Usuarios).
 * Es a la vez modelo de dominio y entidad de Hibernate.
 * Los métodos iniciarSesion / cerrarSesion / validarCredenciales del diagrama viven en
 * application.service.AutenticacionService.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends Auditable {

    /** Hibernate genera el UUID al persistir. */
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    /** Siempre cifrada con BCrypt por el PasswordEncoder. Nunca en texto plano. */
    @Column(nullable = false)
    private String contrasena;

    /** Relación real: muchos usuarios pertenecen a un rol. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    /** true = activo. */
    @Column(nullable = false)
    private boolean estado = true;
}
