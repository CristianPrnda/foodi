package com.university.demo.application.mapper;

import com.university.demo.application.dto.FacturaDto;
import com.university.demo.domain.Factura;
import org.springframework.stereotype.Component;

@Component
public class FacturaMapper {

    public FacturaDto toDto(Factura f) {
        return new FacturaDto(f.getId(), f.getNumero(), f.getFecha(),
                f.getCliente() != null ? f.getCliente().getNombre() : "Consumidor Final",
                f.getDocumentoCliente(), f.getSubtotal(), f.getIva(), f.getTotal(),
                f.getEstado().name(), f.getPedido().getNumeroPedido());
    }
}
