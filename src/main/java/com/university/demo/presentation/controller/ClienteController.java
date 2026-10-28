package com.university.demo.presentation.controller;

import com.university.demo.application.dto.ClienteDto;
import com.university.demo.application.dto.ClienteRequest;
import com.university.demo.application.dto.PedidoDto;
import com.university.demo.application.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public List<ClienteDto> listar(@RequestParam(required = false) String q) {
        return clienteService.listar(q);
    }

    @GetMapping("/{id}")
    public ClienteDto obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDto registrar(@Valid @RequestBody ClienteRequest request) {
        return clienteService.registrar(request);
    }

    @PutMapping("/{id}")
    public ClienteDto actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @GetMapping("/{id}/historial")
    public List<PedidoDto> historial(@PathVariable Long id) {
        return clienteService.historial(id);
    }

    @PostMapping("/{id}/recalcular-total")
    public ClienteDto recalcularTotal(@PathVariable Long id) {
        return clienteService.recalcularTotal(id);
    }
}
