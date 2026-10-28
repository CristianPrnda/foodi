package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cliente del restaurante. registrarCliente() del diagrama = ClienteRepository.save(...). */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Cédula o NIT, ej: "CC 52.778.901". */
    @Column(unique = true, length = 30)
    private String documento;

    @Column(length = 120)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String direccion;

    @Column(nullable = false)
    private int visitas;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalGastado = BigDecimal.ZERO;

    /** Ej: "3,2 sem". */
    @Column(length = 30)
    private String frecuencia;

    @Column(nullable = false)
    private boolean vip;

    /** "realiza": un cliente realiza 0..* pedidos. */
    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();

    /** "recibe": un cliente recibe 0..* facturas. */
    @OneToMany(mappedBy = "cliente")
    private List<Factura> facturas = new ArrayList<>();

    public void actualizarDatos(String nombre, String email, String telefono, String direccion) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public List<Pedido> consultarHistorial() {
        return Collections.unmodifiableList(pedidos);
    }

    /** Suma las facturas no rechazadas y actualiza totalGastado. */
    public BigDecimal calcularTotalGastado() {
        this.totalGastado = facturas.stream()
                .filter(f -> f.getEstado() != EstadoFactura.RECHAZADA)
                .map(Factura::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalGastado;
    }

    public void incrementarVisitas() {
        this.visitas++;
    }
}
