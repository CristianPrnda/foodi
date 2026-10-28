package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Restaurante (sede). Administra sus mesas y posee un inventario. */
@Entity
@Table(name = "restaurantes")
@Getter
@Setter
@NoArgsConstructor
public class Restaurante extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String sede;

    @Column(length = 60)
    private String ciudad;

    @Column(length = 150)
    private String direccion;

    /** Composición 1 — 1..* : "administra". */
    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mesa> mesas = new ArrayList<>();

    /** Composición 1 — 1 : "posee". */
    @OneToOne(mappedBy = "restaurante", cascade = CascadeType.ALL, orphanRemoval = true)
    private Inventario inventario;

    public void registrarMesa(Mesa mesa) {
        mesa.setRestaurante(this);
        mesas.add(mesa);
    }

    public List<Mesa> consultarMesas() {
        return Collections.unmodifiableList(mesas);
    }

    /** Suma de los pedidos de todas las mesas. */
    public BigDecimal consultarVentas() {
        return mesas.stream()
                .flatMap(m -> m.getPedidos().stream())
                .map(Pedido::calcularTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
