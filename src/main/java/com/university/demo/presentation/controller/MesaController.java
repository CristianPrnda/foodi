package com.university.demo.presentation.controller;

import com.university.demo.application.dto.CobroRequest;
import com.university.demo.application.dto.FacturaDto;
import com.university.demo.application.dto.MesaCreateRequest;
import com.university.demo.application.dto.MesaDto;
import com.university.demo.application.service.MesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;

    @GetMapping
    public List<MesaDto> listar() {
        return mesaService.listar();
    }

    @GetMapping("/{id}")
    public MesaDto obtener(@PathVariable Long id) {
        return mesaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MesaDto crear(@Valid @RequestBody MesaCreateRequest request) {
        return mesaService.crear(request);
    }

    @PostMapping("/{id}/ocupar")
    public MesaDto ocupar(@PathVariable Long id) {
        return mesaService.ocupar(id);
    }

    @PostMapping("/{id}/solicitar-pago")
    public MesaDto solicitarPago(@PathVariable Long id) {
        return mesaService.solicitarPago(id);
    }

    @PostMapping("/{id}/liberar")
    public MesaDto liberar(@PathVariable Long id) {
        return mesaService.liberar(id);
    }

    /** Cobra la cuenta, emite la factura y libera la mesa. El cuerpo es opcional. */
    @PostMapping("/{id}/cobrar")
    public FacturaDto cobrar(@PathVariable Long id, @RequestBody(required = false) CobroRequest request) {
        return mesaService.cobrar(id, request);
    }
}
