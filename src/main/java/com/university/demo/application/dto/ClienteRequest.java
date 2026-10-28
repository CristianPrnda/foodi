package com.university.demo.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @Size(max = 30) String documento,
        @Email(message = "Correo inválido") String email,
        @Size(max = 30) String telefono,
        @Size(max = 150) String direccion) {
}
