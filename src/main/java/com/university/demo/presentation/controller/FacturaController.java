package com.university.demo.presentation.controller;

import com.university.demo.application.dto.FacturaCreateRequest;
import com.university.demo.application.dto.FacturaDto;
import com.university.demo.application.dto.FacturacionResumenDto;
import com.university.demo.application.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping
    public List<FacturaDto> listar(@RequestParam(required = false) String q) {
        return facturaService.listar(q);
    }

    @GetMapping("/ultimas")
    public List<FacturaDto> ultimas() {
        return facturaService.ultimas();
    }

    @GetMapping("/resumen")
    public FacturacionResumenDto resumen() {
        return facturaService.resumen();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaDto generar(@Valid @RequestBody FacturaCreateRequest request) {
        return facturaService.generar(request);
    }

    @PostMapping("/{id}/validar-dian")
    public FacturaDto validarDian(@PathVariable Long id) {
        return facturaService.validarDian(id);
    }

    @PostMapping("/{id}/anular")
    public FacturaDto anular(@PathVariable Long id) {
        return facturaService.anular(id);
    }
}
