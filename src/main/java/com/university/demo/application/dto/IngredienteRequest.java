package com.university.demo.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record IngredienteRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @PositiveOrZero(message = "El stock no puede ser negativo") double stock,
        @NotBlank(message = "La unidad es obligatoria") String unidad,
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo") double stockMinimo,
        @DecimalMin(value = "0.0", message = "El costo no puede ser negativo") BigDecimal costoUnitario,
        Long proveedorId) {
}
