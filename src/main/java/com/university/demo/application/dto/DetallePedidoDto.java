package com.university.demo.application.dto;

import java.math.BigDecimal;

public record DetallePedidoDto(Long id, int cantidad, String descripcion, BigDecimal precioUnitario,
                               BigDecimal subtotal) {
}
