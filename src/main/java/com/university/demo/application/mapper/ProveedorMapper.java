package com.university.demo.application.mapper;

import com.university.demo.application.dto.ProveedorDto;
import com.university.demo.domain.Proveedor;
import org.springframework.stereotype.Component;

@Component
public class ProveedorMapper {

    public ProveedorDto toDto(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getNombre(), p.getTelefono(), p.getEmail(), p.getDireccion(),
                p.consultarProductos().size());
    }
}
