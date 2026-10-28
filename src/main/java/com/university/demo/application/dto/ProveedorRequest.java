package com.university.demo.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String telefono,
        @Email(message = "Correo inválido") String email,
        String direccion) {
}
