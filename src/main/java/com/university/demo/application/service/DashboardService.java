package com.university.demo.application.service;

import com.university.demo.application.dto.DashboardDto;
import com.university.demo.application.dto.PrediccionDto;
import com.university.demo.application.dto.VentaDiaDto;
import com.university.demo.application.mapper.PrediccionMapper;
import com.university.demo.domain.EstadoMesa;
import com.university.demo.domain.EstadoPedido;
import com.university.demo.domain.Pedido;
import com.university.demo.domain.PrediccionIA;
import com.university.demo.infrastructure.repository.MesaRepository;
import com.university.demo.infrastructure.repository.PedidoRepository;
import com.university.demo.infrastructure.repository.PrediccionIARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Clase "Dashboard" del diagrama: consulta pedidos, mesas y predicciones y arma los indicadores. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final PrediccionIARepository prediccionRepository;
    private final PrediccionMapper prediccionMapper;

    @Transactional(readOnly = true)
    public DashboardDto mostrarIndicadores() {
        LocalDate hoy = LocalDate.now();
        List<Pedido> pedidosHoy = pedidoRepository.findByFechaBetweenAndEstado(
                hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay(), EstadoPedido.PAGADO);

        BigDecimal ventas = sumar(pedidosHoy);
        int totalPedidos = pedidosHoy.size();
        BigDecimal ticketPromedio = totalPedidos == 0
                ? BigDecimal.ZERO
                : ventas.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);

        long ocupadas = mesaRepository.countByEstado(EstadoMesa.OCUPADA)
                + mesaRepository.countByEstado(EstadoMesa.ESPERANDO_PAGO);

        PrediccionDto prediccion = prediccionRepository.findFirstByOrderByFechaDesc()
                .map(prediccionMapper::toDto)
                .orElse(null);

        return new DashboardDto(ventas, totalPedidos, ticketPromedio, ocupadas, mesaRepository.count(),
                prediccion, ventasUltimosSieteDias(hoy));
    }

    /** Ventas reales de los últimos 7 días (hoy incluido) junto con la predicción guardada de cada día. */
    private List<VentaDiaDto> ventasUltimosSieteDias(LocalDate hoy) {
        LocalDate desde = hoy.minusDays(6);
        List<Pedido> pedidos = pedidoRepository.findByFechaBetweenAndEstado(
                desde.atStartOfDay(), hoy.plusDays(1).atStartOfDay(), EstadoPedido.PAGADO);
        List<VentaDiaDto> semana = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            final LocalDate d = dia;
            BigDecimal real = sumar(pedidos.stream().filter(p -> p.getFecha().toLocalDate().equals(d)).toList());
            BigDecimal predicho = prediccionRepository.findByFecha(d)
                    .map(PrediccionIA::getVentasPredichas).orElse(null);
            String etiqueta = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CO"));
            semana.add(new VentaDiaDto(d, etiqueta, real, predicho));
        }
        return semana;
    }

    private BigDecimal sumar(List<Pedido> pedidos) {
        return pedidos.stream().map(Pedido::getTotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
