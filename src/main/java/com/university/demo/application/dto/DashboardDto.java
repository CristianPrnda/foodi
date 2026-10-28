package com.university.demo.application.dto;

import java.math.BigDecimal;
import java.util.List;

/** Clase "Dashboard" del diagrama: indicadores del día + última predicción (puede ser null). */
public record DashboardDto(BigDecimal ventasDelDia, int totalPedidos, BigDecimal ticketPromedio,
                           long mesasOcupadas, long totalMesas, PrediccionDto prediccion,
                           List<VentaDiaDto> ventasSemana) {
}
