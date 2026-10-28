package com.university.demo.application.dto;

/** estado = LIBRE | OCUPADA | ESPERANDO_PAGO. pedidoActivo es null si la mesa no tiene cuenta abierta. */
public record MesaDto(Long id, int numero, String estado, String tiempoOcupacion, PedidoDto pedidoActivo) {
}
