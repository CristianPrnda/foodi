package com.university.demo.service;

import com.university.demo.application.dto.*;
import com.university.demo.application.service.*;
import com.university.demo.domain.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de los casos de uso con JPA real (H2 + datos del DataLoader).
 * Cada prueba hace rollback al terminar, así que no se contaminan entre sí.
 */
@SpringBootTest
@Transactional
class FlujoServiciosTest {

    @Autowired MesaService mesaService;
    @Autowired PedidoService pedidoService;
    @Autowired FacturaService facturaService;
    @Autowired ClienteService clienteService;
    @Autowired InventarioService inventarioService;
    @Autowired DashboardService dashboardService;

    private MesaDto mesaLibre() {
        return mesaService.listar().stream()
                .filter(m -> "LIBRE".equals(m.estado()))
                .findFirst().orElseThrow();
    }

    private PedidoDto pedidoConUnItem(Long mesaId, Long clienteId) {
        PedidoDto pedido = pedidoService.abrir(new PedidoCreateRequest(mesaId, "Prueba", clienteId));
        return pedidoService.agregarDetalle(pedido.id(), new DetalleRequest("Café", 2, new BigDecimal("5000")));
    }

    private static void assertDinero(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void crearMesaDevuelveSuIdYNoPermiteRepetirNumero() {
        MesaDto mesa = mesaService.crear(new MesaCreateRequest(99));
        assertNotNull(mesa.id());
        assertEquals(99, mesa.numero());
        assertEquals("LIBRE", mesa.estado());

        assertThrows(IllegalArgumentException.class, () -> mesaService.crear(new MesaCreateRequest(99)));
    }

    @Test
    void agregarDetalleAsignaIdYCalculaTotales() {
        PedidoDto pedido = pedidoConUnItem(mesaLibre().id(), null);

        assertEquals(1, pedido.detalles().size());
        assertNotNull(pedido.detalles().get(0).id());
        assertDinero("10000", pedido.subtotal());
        assertDinero("800", pedido.iva());
        assertDinero("1000", pedido.propina());
        assertDinero("11800", pedido.total());
        assertEquals("ABIERTO", pedido.estado());
    }

    @Test
    void cobrarCierraElPedidoFacturaYLiberaLaMesa() {
        MesaDto mesa = mesaLibre();
        PedidoDto pedido = pedidoConUnItem(mesa.id(), null);
        assertEquals("OCUPADA", mesaService.obtener(mesa.id()).estado());

        FacturaDto factura = mesaService.cobrar(mesa.id(), null);

        assertDinero("10800", factura.total());          // subtotal + IVA (la propina no se factura)
        assertEquals("ACEPTADA_DIAN", factura.estado());
        assertEquals("LIBRE", mesaService.obtener(mesa.id()).estado());
        assertEquals("PAGADO", pedidoService.obtener(pedido.id()).estado());
        assertThrows(IllegalStateException.class,
                () -> pedidoService.agregarDetalle(pedido.id(), new DetalleRequest("Tarde", 1, BigDecimal.TEN)));
    }

    @Test
    void facturarUnPedidoTambienLoCierraYLiberaLaMesa() {
        MesaDto mesa = mesaLibre();
        PedidoDto pedido = pedidoConUnItem(mesa.id(), null);

        facturaService.generar(new FacturaCreateRequest(pedido.id()));

        assertEquals("PAGADO", pedidoService.obtener(pedido.id()).estado());
        assertEquals("LIBRE", mesaService.obtener(mesa.id()).estado());
        assertThrows(IllegalStateException.class,
                () -> facturaService.generar(new FacturaCreateRequest(pedido.id())));
    }

    @Test
    void totalGastadoDelClienteNoSeDuplica() {
        ClienteDto cliente = clienteService.registrar(new ClienteRequest("Cliente Prueba", "CC 999.999", null, null, null));
        MesaDto mesa = mesaLibre();
        pedidoConUnItem(mesa.id(), cliente.id());

        mesaService.cobrar(mesa.id(), null);

        ClienteDto actualizado = clienteService.obtener(cliente.id());
        assertEquals(1, actualizado.visitas());
        assertDinero("10800", actualizado.totalGastado());
    }

    @Test
    void noPermiteDosClientesConElMismoDocumento() {
        clienteService.registrar(new ClienteRequest("Uno", "CC 123", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.registrar(new ClienteRequest("Dos", "CC 123", null, null, null)));
    }

    @Test
    void ingredienteNuevoTieneIdYLosMovimientosActualizanElEstado() {
        IngredienteDto sal = inventarioService.agregarIngrediente(
                new IngredienteRequest("Sal de prueba", 10, "kg", 2, new BigDecimal("1000"), null));
        assertNotNull(sal.id());
        assertEquals("NORMAL", sal.estado());

        inventarioService.registrarMovimiento(sal.id(),
                new MovimientoRequest(TipoMovimiento.MERMA, 9, null, "se derramó"));

        assertTrue(inventarioService.alertas().stream().anyMatch(i -> i.id().equals(sal.id())));
        assertThrows(IllegalStateException.class, () -> inventarioService.registrarMovimiento(sal.id(),
                new MovimientoRequest(TipoMovimiento.MERMA, 100, null, null)));
    }

    @Test
    void elDashboardSoloSumaPedidosPagados() {
        BigDecimal antes = dashboardService.mostrarIndicadores().ventasDelDia();
        MesaDto mesa = mesaLibre();
        pedidoConUnItem(mesa.id(), null);

        assertEquals(0, antes.compareTo(dashboardService.mostrarIndicadores().ventasDelDia()),
                "una cuenta abierta no es una venta");

        mesaService.cobrar(mesa.id(), null);
        assertDinero(antes.add(new BigDecimal("11800")).toPlainString(),
                dashboardService.mostrarIndicadores().ventasDelDia());
    }
}
