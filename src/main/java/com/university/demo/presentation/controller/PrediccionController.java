package com.university.demo.presentation.controller;

import com.university.demo.application.dto.PrediccionDto;
import com.university.demo.application.service.PrediccionIAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediccion")
@RequiredArgsConstructor
public class PrediccionController {

    private final PrediccionIAService prediccionService;

    /** Última predicción guardada (204 si todavía no hay ninguna). */
    @GetMapping
    public ResponseEntity<PrediccionDto> ultima() {
        return prediccionService.ultima()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Recalcula la predicción de mañana con el historial de pedidos. */
    @PostMapping("/generar")
    public PrediccionDto generar() {
        return prediccionService.generarPrediccion();
    }
}
