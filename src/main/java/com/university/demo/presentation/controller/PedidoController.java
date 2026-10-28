package com.university.demo.presentation.controller;

import com.university.demo.application.dto.*;
import com.university.demo.application.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public List<PedidoDto> listar() {
        return pedidoService.listarRecientes();
    }

    @GetMapping("/{id}")
    public PedidoDto obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto abrir(@Valid @RequestBody PedidoCreateRequest request) {
        return pedidoService.abrir(request);
    }

    @PostMapping("/{id}/detalles")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto agregarDetalle(@PathVariable Long id, @Valid @RequestBody DetalleRequest request) {
        return pedidoService.agregarDetalle(id, request);
    }

    @PutMapping("/{id}/detalles/{detalleId}")
    public PedidoDto modificarCantidad(@PathVariable Long id, @PathVariable Long detalleId,
                                       @Valid @RequestBody CantidadRequest request) {
        return pedidoService.modificarCantidad(id, detalleId, request);
    }

    @DeleteMapping("/{id}/detalles/{detalleId}")
    public PedidoDto eliminarDetalle(@PathVariable Long id, @PathVariable Long detalleId) {
        return pedidoService.eliminarDetalle(id, detalleId);
    }

    @PatchMapping("/{id}/estado")
    public PedidoDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return pedidoService.cambiarEstado(id, request);
    }
}
