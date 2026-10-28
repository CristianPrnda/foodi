package com.university.demo.application.service;

import com.university.demo.application.dto.PrediccionDto;
import com.university.demo.application.mapper.PrediccionMapper;
import com.university.demo.domain.EstadoPedido;
import com.university.demo.domain.EstadoStock;
import com.university.demo.domain.Ingrediente;
import com.university.demo.domain.Pedido;
import com.university.demo.domain.PrediccionIA;
import com.university.demo.infrastructure.repository.IngredienteRepository;
import com.university.demo.infrastructure.repository.PedidoRepository;
import com.university.demo.infrastructure.repository.PrediccionIARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de predicción de ventas (estadístico, sin librerías externas).
 *
 * generarPrediccion(): para MAÑANA promedia las ventas de los mismos días de la semana de las
 *   últimas 8 semanas y aplica un ajuste de tendencia (últimos 7 días vs. los 7 anteriores, tope ±20%).
 * calcularConfianza(): sube con la cantidad de datos y baja con la variabilidad (0.50 – 0.97).
 * identificarHoraPico(): franja de 2,5 h alrededor de la hora con más ventas históricas.
 * generarRecomendaciones(): meseros extra si la demanda está alta y compras urgentes por stock.
 */
@Service
@RequiredArgsConstructor
public class PrediccionIAService {

    public static final String SEPARADOR = " | ";

    private final PedidoRepository pedidoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final PrediccionIARepository prediccionRepository;
    private final PrediccionMapper prediccionMapper;

    @Transactional(readOnly = true)
    public Optional<PrediccionDto> ultima() {
        return prediccionRepository.findFirstByOrderByFechaDesc().map(prediccionMapper::toDto);
    }

    @Transactional
    public PrediccionDto generarPrediccion() {
        LocalDate hoy = LocalDate.now();
        LocalDate objetivo = hoy.plusDays(1);

        // Historial: hasta 8 semanas atrás, sin contar hoy (el día todavía no termina).
        List<Pedido> historial = pedidoRepository.findByFechaBetweenAndEstado(
                hoy.minusWeeks(8).atStartOfDay(), hoy.atStartOfDay(), EstadoPedido.PAGADO);
        Map<LocalDate, BigDecimal> ventasPorDia = historial.stream().collect(Collectors.groupingBy(
                p -> p.getFecha().toLocalDate(),
                TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Pedido::getTotal, BigDecimal::add)));

        List<Double> mismosDias = ventasPorDia.entrySet().stream()
                .filter(e -> e.getKey().getDayOfWeek() == objetivo.getDayOfWeek())
                .map(e -> e.getValue().doubleValue()).toList();
        List<Double> muestra = mismosDias.size() >= 2
                ? mismosDias
                : ventasPorDia.values().stream().map(BigDecimal::doubleValue).toList();

        double promedio = muestra.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double ajuste = 1 + tendencia(ventasPorDia, hoy);
        BigDecimal ventas = BigDecimal.valueOf(promedio * ajuste).setScale(0, RoundingMode.HALF_UP);

        double confianza = calcularConfianza(muestra);
        String horaPico = identificarHoraPico(historial);
        double promedioGeneral = ventasPorDia.values().stream()
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        String recomendacion = generarRecomendaciones(ventas.doubleValue(), promedioGeneral);

        PrediccionIA prediccion = prediccionRepository.findByFecha(objetivo).orElseGet(PrediccionIA::new);
        prediccion.setFecha(objetivo);
        prediccion.setVentasPredichas(ventas);
        prediccion.setNivelConfianza(confianza);
        prediccion.setHoraPico(horaPico);
        prediccion.setRecomendacion(recomendacion.length() > 500 ? recomendacion.substring(0, 500) : recomendacion);
        return prediccionMapper.toDto(prediccionRepository.save(prediccion));
    }

    /** Variación (−0,2 … +0,2) entre los últimos 7 días y los 7 anteriores. */
    double tendencia(Map<LocalDate, BigDecimal> ventasPorDia, LocalDate hoy) {
        double reciente = sumar(ventasPorDia, hoy.minusDays(7), hoy);
        double previo = sumar(ventasPorDia, hoy.minusDays(14), hoy.minusDays(7));
        if (previo <= 0 || reciente <= 0) {
            return 0;
        }
        return Math.max(-0.2, Math.min(0.2, (reciente - previo) / previo));
    }

    /** Nivel de confianza entre 0.50 y 0.97 según cantidad de datos y variabilidad. */
    public double calcularConfianza(List<Double> muestra) {
        if (muestra.isEmpty()) {
            return 0.50;
        }
        double media = muestra.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double varianza = muestra.stream().mapToDouble(v -> Math.pow(v - media, 2)).average().orElse(0);
        double coeficiente = media == 0 ? 1 : Math.sqrt(varianza) / media;
        double valor = 0.55 + 0.05 * Math.min(muestra.size(), 8) - 0.3 * Math.min(coeficiente, 0.5);
        return Math.round(Math.max(0.50, Math.min(0.97, valor)) * 100) / 100.0;
    }

    /** Ej: "19:00 - 21:30". Si no hay historial devuelve la franja típica de cena. */
    public String identificarHoraPico(List<Pedido> historial) {
        if (historial.isEmpty()) {
            return "19:00 - 21:30";
        }
        Map<Integer, BigDecimal> porHora = historial.stream().collect(Collectors.groupingBy(
                p -> p.getFecha().getHour(),
                Collectors.reducing(BigDecimal.ZERO, Pedido::getTotal, BigDecimal::add)));
        int hora = Collections.max(porHora.entrySet(), Map.Entry.comparingByValue()).getKey();
        int fin = Math.min(hora + 2, 23);
        return String.format("%02d:00 - %02d:30", hora, fin);
    }

    /** Recomendaciones separadas por " | " (el front las muestra como tarjetas). */
    public String generarRecomendaciones(double ventasPredichas, double promedioGeneral) {
        List<String> recomendaciones = new ArrayList<>();
        if (promedioGeneral > 0 && ventasPredichas > promedioGeneral * 1.15) {
            recomendaciones.add("Agenda 1 mesero más: demanda esperada +"
                    + Math.round((ventasPredichas / promedioGeneral - 1) * 100) + "% sobre el promedio");
        } else if (promedioGeneral > 0 && ventasPredichas < promedioGeneral * 0.85) {
            recomendaciones.add("Día tranquilo: reduce compras de perecederos y considera una promoción");
        } else {
            recomendaciones.add("Demanda normal: mantén el personal habitual");
        }
        List<Ingrediente> criticos = ingredienteRepository.findByEstado(EstadoStock.CRITICO);
        if (!criticos.isEmpty()) {
            recomendaciones.add("Compra urgente: " + criticos.stream()
                    .map(Ingrediente::getNombre).limit(3).collect(Collectors.joining(", ")));
        } else {
            List<Ingrediente> bajos = ingredienteRepository.findByEstado(EstadoStock.BAJO);
            if (!bajos.isEmpty()) {
                recomendaciones.add("Reponer pronto: " + bajos.stream()
                        .map(Ingrediente::getNombre).limit(3).collect(Collectors.joining(", ")));
            }
        }
        return String.join(SEPARADOR, recomendaciones);
    }

    private double sumar(Map<LocalDate, BigDecimal> ventas, LocalDate desde, LocalDate hastaExcl) {
        return ventas.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(desde) && e.getKey().isBefore(hastaExcl))
                .mapToDouble(e -> e.getValue().doubleValue()).sum();
    }
}
