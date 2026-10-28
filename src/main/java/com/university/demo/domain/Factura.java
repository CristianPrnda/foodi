package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
public class Factura extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ej: FE-2107, POS-1054. */
    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    /** Cédula/NIT del cliente o "Consumidor Final". */
    @Column(length = 40)
    private String documentoCliente;

    /** "genera": un pedido genera 0..1 factura. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    /** "recibe": un cliente recibe 0..* facturas. Null = consumidor final. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Toma los valores del pedido asociado. */
    public void generarFactura() {
        this.subtotal = pedido.calcularSubtotal();
        this.iva = pedido.calcularIVA();
        this.total = calcularTotal();
        this.fecha = LocalDateTime.now();
        this.estado = EstadoFactura.PENDIENTE;
        this.documentoCliente = cliente != null ? cliente.getDocumento() : "Consumidor Final";
    }

    public BigDecimal calcularTotal() {
        return subtotal.add(iva);
    }

    /**
     * Validación simulada contra la DIAN: acepta si hay documento del cliente.
     * TODO: reemplazar por la integración real con el proveedor tecnológico.
     */
    public boolean validarDIAN() {
        boolean valido = documentoCliente != null && !documentoCliente.isBlank();
        this.estado = valido ? EstadoFactura.ACEPTADA_DIAN : EstadoFactura.RECHAZADA;
        return valido;
    }

    public void anularFactura() {
        this.estado = EstadoFactura.RECHAZADA;
    }
}
