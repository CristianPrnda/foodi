package com.university.demo.application.dto;

import jakarta.validation.constraints.NotNull;

public record FacturaCreateRequest(@NotNull(message = "El pedido es obligatorio") Long pedidoId) {
}
