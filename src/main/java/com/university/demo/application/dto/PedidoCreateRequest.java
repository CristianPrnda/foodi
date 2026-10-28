package com.university.demo.application.dto;

import jakarta.validation.constraints.NotNull;

public record PedidoCreateRequest(
        @NotNull(message = "La mesa es obligatoria") Long mesaId,
        String mesero,
        Long clienteId) {
}
