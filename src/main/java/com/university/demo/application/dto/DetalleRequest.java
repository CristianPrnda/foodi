package com.university.demo.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DetalleRequest(
        @NotBlank(message = "La descripción es obligatoria") String descripcion,
        @Min(value = 1, message = "La cantidad debe ser mínimo 1") int cantidad,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") BigDecimal precioUnitario) {
}
