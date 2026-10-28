package com.university.demo.application.dto;

import java.math.BigDecimal;

public record IngredienteDto(Long id, String nombre, double stock, String unidad, double stockMinimo,
                             BigDecimal costoUnitario, String estado, Long proveedorId, String proveedor) {
}
