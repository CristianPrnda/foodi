package com.university.demo.application.dto;

import java.math.BigDecimal;

public record FacturacionResumenDto(BigDecimal totalVentas, BigDecimal ivaRecaudado, long totalFacturas,
                                    long aceptadas, long rechazadas, long pendientes) {
}
