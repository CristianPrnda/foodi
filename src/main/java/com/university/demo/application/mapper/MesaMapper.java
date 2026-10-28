package com.university.demo.application.mapper;

import com.university.demo.application.dto.MesaDto;
import com.university.demo.domain.Mesa;
import com.university.demo.domain.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MesaMapper {

    private final PedidoMapper pedidoMapper;

    public MesaDto toDto(Mesa mesa, Pedido pedidoActivo) {
        return new MesaDto(mesa.getId(), mesa.getNumero(), mesa.getEstado().name(),
                mesa.getTiempoOcupacion(),
                pedidoActivo != null ? pedidoMapper.toDto(pedidoActivo) : null);
    }
}
