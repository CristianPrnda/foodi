package com.university.demo.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FacturaDto(Long id, String numero, LocalDateTime fecha, String cliente, String documento,
                         BigDecimal subtotal, BigDecimal iva, BigDecimal total, String estado, String pedido) {
}
