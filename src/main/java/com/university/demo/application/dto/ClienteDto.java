package com.university.demo.application.dto;

import java.math.BigDecimal;

public record ClienteDto(Long id, String nombre, String documento, String email, String telefono,
                         String direccion, int visitas, BigDecimal totalGastado, String frecuencia, boolean vip) {
}
