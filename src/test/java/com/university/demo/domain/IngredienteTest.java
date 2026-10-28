package com.university.demo.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredienteTest {

    private Ingrediente ingrediente(double stock, double minimo) {
        Ingrediente i = new Ingrediente();
        i.setNombre("Arroz");
        i.setStock(stock);
        i.setStockMinimo(minimo);
        i.recalcularEstado();
        return i;
    }

    @Test
    void estadoSegunStockMinimo() {
        assertEquals(EstadoStock.NORMAL, ingrediente(30, 25).getEstado());
        assertEquals(EstadoStock.BAJO, ingrediente(20, 25).getEstado());
        assertEquals(EstadoStock.CRITICO, ingrediente(2, 25).getEstado());
    }

    @Test
    void actualizarStockSumaYRecalculaEstado() {
        Ingrediente i = ingrediente(2, 25);
        i.actualizarStock(30);
        assertEquals(32, i.getStock());
        assertEquals(EstadoStock.NORMAL, i.getEstado());
        assertTrue(i.verificarStock());
    }

    @Test
    void noPermiteStockNegativo() {
        Ingrediente i = ingrediente(5, 3);
        assertThrows(IllegalStateException.class, () -> i.actualizarStock(-6));
    }

    @Test
    void movimientoAjusteFijaElStock() {
        Ingrediente i = ingrediente(5, 3);
        MovimientoInventario m = new MovimientoInventario();
        m.setIngrediente(i);
        m.setTipo(TipoMovimiento.AJUSTE);
        m.setCantidad(12);
        m.registrar();
        m.aplicarMovimiento();

        assertEquals(12, i.getStock());
        assertNotNull(m.getFecha());
    }

    @Test
    void movimientoMermaResta() {
        Ingrediente i = ingrediente(10, 3);
        MovimientoInventario m = new MovimientoInventario();
        m.setIngrediente(i);
        m.setTipo(TipoMovimiento.MERMA);
        m.setCantidad(4);
        m.aplicarMovimiento();

        assertEquals(6, i.getStock());
    }
}
