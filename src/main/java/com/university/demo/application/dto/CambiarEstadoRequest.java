package com.university.demo.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(@NotBlank(message = "El estado es obligatorio") String estado) {
}
