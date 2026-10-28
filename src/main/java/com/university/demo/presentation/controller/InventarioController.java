package com.university.demo.presentation.controller;

import com.university.demo.application.dto.IngredienteDto;
import com.university.demo.application.dto.IngredienteRequest;
import com.university.demo.application.dto.MovimientoDto;
import com.university.demo.application.dto.MovimientoRequest;
import com.university.demo.application.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping("/ingredientes")
    public List<IngredienteDto> stock() {
        return inventarioService.consultarStock();
    }

    @GetMapping("/alertas")
    public List<IngredienteDto> alertas() {
        return inventarioService.alertas();
    }

    @PostMapping("/ingredientes")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredienteDto agregar(@Valid @RequestBody IngredienteRequest request) {
        return inventarioService.agregarIngrediente(request);
    }

    @GetMapping("/ingredientes/{id}/movimientos")
    public List<MovimientoDto> movimientos(@PathVariable Long id) {
        return inventarioService.movimientos(id);
    }

    @PostMapping("/ingredientes/{id}/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoDto registrarMovimiento(@PathVariable Long id, @Valid @RequestBody MovimientoRequest request) {
        return inventarioService.registrarMovimiento(id, request);
    }
}
