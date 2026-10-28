package com.university.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Predicción de ventas generada por el módulo de Inteligencia Artificial.
 * Aquí solo se guardan los resultados; el motor que los calcula (generarPrediccion,
 * calcularConfianza, identificarHoraPico, generarRecomendaciones) se construye aparte.
 */
@Entity
@Table(name = "predicciones_ia", uniqueConstraints = @UniqueConstraint(
        name = "uk_prediccion_fecha", columnNames = "fecha"))
@Getter
@Setter
@NoArgsConstructor
public class PrediccionIA extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Día para el que se predice (una sola predicción por día). */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ventasPredichas = BigDecimal.ZERO;

    /** Entre 0 y 1 (0.92 = 92%). */
    @Column(nullable = false)
    private double nivelConfianza;

    /** Ej: "19:00 - 21:30". */
    @Column(length = 30)
    private String horaPico;

    @Column(length = 500)
    private String recomendacion;
}
