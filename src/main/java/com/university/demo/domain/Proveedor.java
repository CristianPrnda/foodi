package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Proveedor de ingredientes. registrarProveedor() del diagrama = ProveedorRepository.save(...). */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(length = 150)
    private String direccion;

    /** "suministra": un proveedor suministra 0..* ingredientes. */
    @OneToMany(mappedBy = "proveedor")
    private List<Ingrediente> ingredientes = new ArrayList<>();

    public void actualizarDatos(String nombre, String telefono, String email, String direccion) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
    }

    public List<Ingrediente> consultarProductos() {
        return Collections.unmodifiableList(ingredientes);
    }
}
