package com.university.demo.infrastructure.repository;

import com.university.demo.domain.EstadoPedido;
import com.university.demo.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    /** Ventas reales: solo pedidos en el estado indicado (PAGADO) dentro del rango. */
    List<Pedido> findByFechaBetweenAndEstado(LocalDateTime desde, LocalDateTime hasta, EstadoPedido estado);

    List<Pedido> findTop50ByOrderByFechaDesc();

    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);

    /** Pedido "vivo" de una mesa: el más reciente que no tenga el estado indicado (normalmente PAGADO). */
    Optional<Pedido> findFirstByMesaIdAndEstadoNotOrderByFechaDesc(Long mesaId, EstadoPedido estado);

    boolean existsByNumeroPedido(String numeroPedido);
}
