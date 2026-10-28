package com.university.demo.application.dto;

import com.university.demo.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDto(Long id, TipoMovimiento tipo, double cantidad, BigDecimal costoUnitario,
                            LocalDateTime fecha, String observaciones, String ingrediente) {
}
