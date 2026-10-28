package com.university.demo.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDto(Long id, String numeroPedido, LocalDateTime fecha, String estado, String mesero,
                        Long mesaId, Integer mesaNumero, Long clienteId, String cliente,
                        List<DetallePedidoDto> detalles, BigDecimal subtotal, BigDecimal iva,
                        BigDecimal propina, BigDecimal total) {
}
