package com.university.demo.application.dto;

import jakarta.validation.constraints.Min;

public record CantidadRequest(@Min(value = 1, message = "La cantidad debe ser mínimo 1") int cantidad) {
}
