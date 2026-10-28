package com.university.demo.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias puras (sin Spring): la lógica del diagrama de clases. */
class PedidoTest {

    private DetallePedido detalle(String nombre, int cantidad, String precio) {
        DetallePedido d = new DetallePedido();
        d.setDescripcion(nombre);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal(precio));
        return d;
    }

    @Test
    void calculaSubtotalIvaPropinaYTotal() {
        Pedido pedido = new Pedido();
        pedido.agregarDetalle(detalle("Bandeja paisa", 2, "32000"));   // 64.000
        pedido.agregarDetalle(detalle("Limonada", 1, "8000"));         //  8.000

        assertEquals(0, new BigDecimal("72000").compareTo(pedido.getSubtotal()));
        assertEquals(new BigDecimal("5760.00"), pedido.getIva());       // 8 %
        assertEquals(new BigDecimal("7200.00"), pedido.getPropina());   // 10 %
        assertEquals(0, new BigDecimal("84960").compareTo(pedido.getTotal()));
    }

    @Test
    void modificarCantidadRecalculaTotales() {
        Pedido pedido = new Pedido();
        DetallePedido d = detalle("Ajiaco", 1, "26000");
        pedido.agregarDetalle(d);
        d.modificarCantidad(3);

        assertEquals(0, new BigDecimal("78000").compareTo(pedido.getSubtotal()));
    }

    @Test
    void cantidadCeroEsInvalida() {
        DetallePedido d = detalle("Ajiaco", 1, "26000");
        assertThrows(IllegalArgumentException.class, () -> d.modificarCantidad(0));
    }

    @Test
    void eliminarDetalleRecalcula() {
        Pedido pedido = new Pedido();
        DetallePedido a = detalle("A", 1, "1000");
        DetallePedido b = detalle("B", 1, "2000");
        pedido.agregarDetalle(a);
        pedido.agregarDetalle(b);
        pedido.eliminarDetalle(a);

        assertEquals(0, new BigDecimal("2000").compareTo(pedido.getSubtotal()));
    }

    @Test
    void estadoInicialEsAbiertoYSePuedeCambiar() {
        Pedido pedido = new Pedido();
        assertEquals(EstadoPedido.ABIERTO, pedido.getEstado());
        assertFalse(pedido.estaPagado());

        pedido.cambiarEstado(EstadoPedido.PAGADO);
        assertTrue(pedido.estaPagado());
    }
}
