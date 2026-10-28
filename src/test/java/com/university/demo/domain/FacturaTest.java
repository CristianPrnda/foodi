package com.university.demo.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FacturaTest {

    private Factura facturaConPedido(Cliente cliente) {
        Pedido pedido = new Pedido();
        DetallePedido d = new DetallePedido();
        d.setDescripcion("Bandeja paisa");
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("100000"));
        pedido.agregarDetalle(d);

        Factura f = new Factura();
        f.setPedido(pedido);
        f.setCliente(cliente);
        f.generarFactura();
        return f;
    }

    @Test
    void generarFacturaToma8PorCientoDeIva() {
        Factura f = facturaConPedido(null);
        assertEquals(0, new BigDecimal("100000").compareTo(f.getSubtotal()));
        assertEquals(0, new BigDecimal("8000").compareTo(f.getIva()));
        assertEquals(0, new BigDecimal("108000").compareTo(f.getTotal()));
        assertEquals("Consumidor Final", f.getDocumentoCliente());
    }

    @Test
    void validarDianAceptaSiHayDocumento() {
        Factura f = facturaConPedido(null);
        assertTrue(f.validarDIAN());
        assertEquals(EstadoFactura.ACEPTADA_DIAN, f.getEstado());
    }

    @Test
    void validarDianRechazaSinDocumento() {
        Factura f = facturaConPedido(null);
        f.setDocumentoCliente(" ");
        assertFalse(f.validarDIAN());
        assertEquals(EstadoFactura.RECHAZADA, f.getEstado());
    }

    @Test
    void totalGastadoIgnoraFacturasRechazadas() {
        Cliente c = new Cliente();
        c.setDocumento("CC 1");
        Factura buena = facturaConPedido(c);
        Factura mala = facturaConPedido(c);
        mala.anularFactura();
        c.getFacturas().add(buena);
        c.getFacturas().add(mala);

        assertEquals(0, new BigDecimal("108000").compareTo(c.calcularTotalGastado()));
    }
}
