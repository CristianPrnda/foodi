package com.university.demo.application.mapper;

import com.university.demo.application.dto.ClienteDto;
import com.university.demo.application.dto.ClienteRequest;
import com.university.demo.domain.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteDto toDto(Cliente c) {
        return new ClienteDto(c.getId(), c.getNombre(), c.getDocumento(), c.getEmail(), c.getTelefono(),
                c.getDireccion(), c.getVisitas(), c.getTotalGastado(), c.getFrecuencia(), c.isVip());
    }

    public Cliente toEntity(ClienteRequest r) {
        Cliente c = new Cliente();
        c.setDocumento(normalizar(r.documento()));
        c.actualizarDatos(r.nombre(), r.email(), r.telefono(), r.direccion());
        return c;
    }

    /** El documento es único: un texto vacío se guarda como null para no chocar con otros vacíos. */
    public static String normalizar(String documento) {
        return documento == null || documento.isBlank() ? null : documento.trim();
    }
}
