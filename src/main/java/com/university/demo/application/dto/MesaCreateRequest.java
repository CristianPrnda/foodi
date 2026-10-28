package com.university.demo.application.dto;

import jakarta.validation.constraints.Min;

public record MesaCreateRequest(@Min(value = 1, message = "El número de mesa debe ser mayor que cero") int numero) {
}
