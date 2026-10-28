package com.university.demo.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MesaTest {

    @Test
    void cicloDeVidaDeUnaMesa() {
        Mesa mesa = new Mesa();
        assertEquals(EstadoMesa.LIBRE, mesa.consultarEstado());
        assertEquals("—", mesa.getTiempoOcupacion());

        mesa.ocupar();
        assertEquals(EstadoMesa.OCUPADA, mesa.consultarEstado());
        assertNotNull(mesa.getOcupadaDesde());

        mesa.solicitarPago();
        assertEquals(EstadoMesa.ESPERANDO_PAGO, mesa.consultarEstado());

        mesa.liberar();
        assertEquals(EstadoMesa.LIBRE, mesa.consultarEstado());
        assertNull(mesa.getOcupadaDesde());
    }
}
