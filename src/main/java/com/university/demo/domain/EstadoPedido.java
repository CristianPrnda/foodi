package com.university.demo.domain;

/** Ciclo de vida de un pedido. PAGADO solo se alcanza al cobrar/facturar. */
public enum EstadoPedido {
    ABIERTO,
    EN_COCINA,
    SERVIDO,
    PAGADO
}
