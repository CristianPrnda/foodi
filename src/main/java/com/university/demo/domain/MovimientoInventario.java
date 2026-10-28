package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /** En ENTRADA y MERMA es la cantidad que entra/sale; en AJUSTE es el nuevo stock. */
    @Column(nullable = false)
    private double cantidad;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 250)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    /** Deja listo el movimiento (pone la fecha si falta). Guardarlo = MovimientoInventarioRepository.save(...). */
    public void registrar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    /** Modifica el stock del ingrediente según el tipo de movimiento. */
    public void aplicarMovimiento() {
        switch (tipo) {
            case ENTRADA -> ingrediente.actualizarStock(cantidad);
            case MERMA -> ingrediente.actualizarStock(-cantidad);
            case AJUSTE -> ingrediente.actualizarStock(cantidad - ingrediente.getStock());
        }
    }
}
