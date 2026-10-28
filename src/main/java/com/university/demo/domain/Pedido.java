package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedidos_fecha", columnList = "fecha"),
        @Index(name = "idx_pedidos_mesa_estado", columnList = "mesa_id, estado")
})
@Getter
@Setter
@NoArgsConstructor
public class Pedido extends Auditable {

    /** Impuesto al consumo (ver HTML: 8%). Ajustable. */
    public static final BigDecimal TARIFA_IVA = new BigDecimal("0.08");
    /** Propina sugerida (10%). */
    public static final BigDecimal TARIFA_PROPINA = new BigDecimal("0.10");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroPedido;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.ABIERTO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal propina = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 80)
    private String mesero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    /** Puede ser null (consumidor final). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** "contiene": composición 1 — 1..*. */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    /** "genera": un pedido genera 0..1 factura. */
    @OneToOne(mappedBy = "pedido")
    private Factura factura;

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
        recalcularTotales();
    }

    public void eliminarDetalle(DetallePedido detalle) {
        if (detalles.remove(detalle)) {
            detalle.setPedido(null);
            recalcularTotales();
        }
    }

    public BigDecimal calcularSubtotal() {
        return detalles.stream()
                .map(DetallePedido::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularIVA() {
        return calcularSubtotal().multiply(TARIFA_IVA).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularPropina() {
        return calcularSubtotal().multiply(TARIFA_PROPINA).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTotal() {
        return calcularSubtotal().add(calcularIVA()).add(calcularPropina());
    }

    public void cambiarEstado(EstadoPedido nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public boolean estaPagado() {
        return estado == EstadoPedido.PAGADO;
    }

    /** Actualiza los campos guardados (subtotal, iva, propina, total). */
    public void recalcularTotales() {
        this.subtotal = calcularSubtotal();
        this.iva = calcularIVA();
        this.propina = calcularPropina();
        this.total = calcularTotal();
    }
}
