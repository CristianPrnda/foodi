package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "inventarios")
@Getter
@Setter
@NoArgsConstructor
public class Inventario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** El restaurante "posee" este inventario. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurante_id", nullable = false, unique = true)
    private Restaurante restaurante;

    /** "contiene": composición 1 — 1..*. */
    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ingrediente> ingredientes = new ArrayList<>();

    /** Diagrama: cantidadIngredientes : int (se calcula, no se guarda). */
    public int getCantidadIngredientes() {
        return ingredientes.size();
    }

    public void agregarIngrediente(Ingrediente ingrediente) {
        ingrediente.setInventario(this);
        ingredientes.add(ingrediente);
    }

    public void eliminarIngrediente(Ingrediente ingrediente) {
        if (ingredientes.remove(ingrediente)) {
            ingrediente.setInventario(null);
        }
    }

    public List<Ingrediente> consultarStock() {
        return Collections.unmodifiableList(ingredientes);
    }

    /** Aplica el movimiento (entrada, merma, ajuste) sobre el ingrediente. */
    public void registrarMovimiento(MovimientoInventario movimiento) {
        movimiento.registrar();
        movimiento.aplicarMovimiento();
    }

    /** Ingredientes por debajo de su stock mínimo. */
    public List<Ingrediente> verificarStockMinimo() {
        return ingredientes.stream().filter(i -> !i.verificarStock()).toList();
    }
}
