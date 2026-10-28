package com.university.demo.presentation.controller;

import com.university.demo.application.dto.IngredienteDto;
import com.university.demo.application.dto.ProveedorDto;
import com.university.demo.application.dto.ProveedorRequest;
import com.university.demo.application.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public List<ProveedorDto> listar() {
        return proveedorService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorDto registrar(@Valid @RequestBody ProveedorRequest request) {
        return proveedorService.registrar(request);
    }

    @PutMapping("/{id}")
    public ProveedorDto actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return proveedorService.actualizar(id, request);
    }

    @GetMapping("/{id}/productos")
    public List<IngredienteDto> productos(@PathVariable Long id) {
        return proveedorService.productos(id);
    }
}
