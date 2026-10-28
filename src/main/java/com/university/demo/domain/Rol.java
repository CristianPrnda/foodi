package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rol de un usuario (ADMINISTRADOR, CAJERO, MESERO...). Muchos usuarios pertenecen a un rol. */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Rol extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    public Rol(String nombre) {
        this.nombre = nombre;
    }
}
