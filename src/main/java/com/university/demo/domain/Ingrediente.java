package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ingredientes")
@Getter
@Setter
@NoArgsConstructor
public class Ingrediente extends Auditable {

    /** Por debajo de esta fracción del stock mínimo el estado es CRITICO. Ajustable. */
    private static final double UMBRAL_CRITICO = 0.35;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private double stock;

    /** kg, un., bot., l ... */
    @Column(nullable = false, length = 20)
    private String unidad;

    @Column(nullable = false)
    private double stockMinimo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoStock estado = EstadoStock.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_id", nullable = false)
    private Inventario inventario;

    /** "suministra". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /** "registra": un ingrediente registra 0..* movimientos. */
    @OneToMany(mappedBy = "ingrediente")
    private List<MovimientoInventario> movimientos = new ArrayList<>();

    /** Suma (o resta, si es negativa) la cantidad al stock y recalcula el estado. */
    public void actualizarStock(double cantidad) {
        double nuevoStock = this.stock + cantidad;
        if (nuevoStock < 0) {
            throw new IllegalStateException("Stock insuficiente de " + nombre);
        }
        this.stock = nuevoStock;
        recalcularEstado();
    }

    /** true si el stock está en o sobre el mínimo. */
    public boolean verificarStock() {
        return stock >= stockMinimo;
    }

    /** Valor en dinero del stock actual. */
    public BigDecimal calcularCosto() {
        return costoUnitario.multiply(BigDecimal.valueOf(stock));
    }

    public void recalcularEstado() {
        if (stock < stockMinimo * UMBRAL_CRITICO) {
            this.estado = EstadoStock.CRITICO;
        } else if (stock < stockMinimo) {
            this.estado = EstadoStock.BAJO;
        } else {
            this.estado = EstadoStock.NORMAL;
        }
    }
}
