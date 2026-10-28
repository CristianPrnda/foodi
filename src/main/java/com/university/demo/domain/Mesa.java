package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mesas", uniqueConstraints = @UniqueConstraint(
        name = "uk_mesa_restaurante_numero", columnNames = {"restaurante_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class Mesa extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMesa estado = EstadoMesa.LIBRE;

    /** Momento en que se ocupó la mesa. De aquí se calcula tiempoOcupacion. */
    private Instant ocupadaDesde;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurante_id", nullable = false)
    private Restaurante restaurante;

    /** Una mesa tiene 0..* pedidos. */
    @OneToMany(mappedBy = "mesa")
    private List<Pedido> pedidos = new ArrayList<>();

    public void ocupar() {
        this.estado = EstadoMesa.OCUPADA;
        this.ocupadaDesde = Instant.now();
    }

    public void liberar() {
        this.estado = EstadoMesa.LIBRE;
        this.ocupadaDesde = null;
    }

    public void solicitarPago() {
        this.estado = EstadoMesa.ESPERANDO_PAGO;
    }

    public EstadoMesa consultarEstado() {
        return estado;
    }

    /** Texto tipo "45m" o "1h 18m" (diagrama: tiempoOcupacion : String). */
    public String getTiempoOcupacion() {
        if (ocupadaDesde == null) {
            return "—";
        }
        Duration d = Duration.between(ocupadaDesde, Instant.now());
        long horas = d.toHours();
        int minutos = d.toMinutesPart();
        return horas > 0 ? horas + "h " + String.format("%02d", minutos) + "m" : minutos + "m";
    }
}
