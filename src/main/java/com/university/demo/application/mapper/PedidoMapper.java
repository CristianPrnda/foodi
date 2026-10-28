package com.university.demo.application.mapper;

import com.university.demo.application.dto.DetallePedidoDto;
import com.university.demo.application.dto.PedidoDto;
import com.university.demo.domain.DetallePedido;
import com.university.demo.domain.Pedido;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {

    public PedidoDto toDto(Pedido p) {
        return new PedidoDto(
                p.getId(), p.getNumeroPedido(), p.getFecha(), p.getEstado().name(), p.getMesero(),
                p.getMesa() != null ? p.getMesa().getId() : null,
                p.getMesa() != null ? p.getMesa().getNumero() : null,
                p.getCliente() != null ? p.getCliente().getId() : null,
                p.getCliente() != null ? p.getCliente().getNombre() : null,
                p.getDetalles().stream().map(this::toDto).toList(),
                p.getSubtotal(), p.getIva(), p.getPropina(), p.getTotal());
    }

    public DetallePedidoDto toDto(DetallePedido d) {
        return new DetallePedidoDto(d.getId(), d.getCantidad(), d.getDescripcion(),
                d.getPrecioUnitario(), d.getSubtotal());
    }
}
