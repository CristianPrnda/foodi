package com.university.demo.application.dto;

import com.university.demo.domain.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** ENTRADA y MERMA: cantidad que entra/sale. AJUSTE: nuevo stock total. */
public record MovimientoRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio") TipoMovimiento tipo,
        @PositiveOrZero(message = "La cantidad no puede ser negativa") double cantidad,
        BigDecimal costoUnitario,
        String observaciones) {
}
