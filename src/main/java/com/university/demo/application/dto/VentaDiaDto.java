package com.university.demo.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un punto del gráfico "Tendencia de ventas": ventas reales vs. predicción IA. */
public record VentaDiaDto(LocalDate fecha, String dia, BigDecimal ventas, BigDecimal prediccion) {
}
