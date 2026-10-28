package com.university.demo.application.service;

import com.university.demo.application.dto.IngredienteDto;
import com.university.demo.application.dto.ProveedorDto;
import com.university.demo.application.dto.ProveedorRequest;
import com.university.demo.application.mapper.InventarioMapper;
import com.university.demo.application.mapper.ProveedorMapper;
import com.university.demo.domain.Proveedor;
import com.university.demo.infrastructure.repository.ProveedorRepository;
import com.university.demo.presentation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso de Proveedor: registrarProveedor, actualizarDatos, consultarProductos. */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;
    private final InventarioMapper inventarioMapper;

    @Transactional(readOnly = true)
    public List<ProveedorDto> listar() {
        return proveedorRepository.findAll().stream().map(proveedorMapper::toDto).toList();
    }

    @Transactional
    public ProveedorDto registrar(ProveedorRequest request) {
        Proveedor p = new Proveedor();
        p.actualizarDatos(request.nombre(), request.telefono(), request.email(), request.direccion());
        return proveedorMapper.toDto(proveedorRepository.save(p));
    }

    @Transactional
    public ProveedorDto actualizar(Long id, ProveedorRequest request) {
        Proveedor p = buscar(id);
        p.actualizarDatos(request.nombre(), request.telefono(), request.email(), request.direccion());
        return proveedorMapper.toDto(p);
    }

    @Transactional(readOnly = true)
    public List<IngredienteDto> productos(Long id) {
        return buscar(id).consultarProductos().stream().map(inventarioMapper::toDto).toList();
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
    }
}
