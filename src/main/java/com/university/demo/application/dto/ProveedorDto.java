package com.university.demo.application.dto;

public record ProveedorDto(Long id, String nombre, String telefono, String email, String direccion,
                           int totalProductos) {
}
