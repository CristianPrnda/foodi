package com.university.demo.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrediccionDto(LocalDate fecha, BigDecimal ventasPredichas, double nivelConfianza,
                            String horaPico, String recomendacion) {
}
