package com.university.demo.application.service;

import com.university.demo.application.dto.FacturaCreateRequest;
import com.university.demo.application.dto.FacturaDto;
import com.university.demo.application.dto.FacturacionResumenDto;
import com.university.demo.application.mapper.FacturaMapper;
import com.university.demo.domain.Cliente;
import com.university.demo.domain.EstadoFactura;
import com.university.demo.domain.EstadoPedido;
import com.university.demo.domain.Factura;
import com.university.demo.domain.Pedido;
import com.university.demo.infrastructure.repository.FacturaRepository;
import com.university.demo.presentation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Casos de uso de Factura: generarFactura, validarDIAN, anularFactura y resumen de facturación. */
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final PedidoService pedidoService;
    private final FacturaMapper facturaMapper;

    @Transactional(readOnly = true)
    public List<FacturaDto> listar(String busqueda) {
        String q = busqueda == null ? "" : busqueda.trim().toLowerCase();
        return facturaRepository.findAllByOrderByFechaDesc().stream()
                .map(facturaMapper::toDto)
                .filter(f -> q.isEmpty()
                        || f.numero().toLowerCase().contains(q)
                        || f.cliente().toLowerCase().contains(q)
                        || (f.documento() != null && f.documento().toLowerCase().contains(q)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FacturaDto> ultimas() {
        return facturaRepository.findTop5ByOrderByFechaDesc().stream().map(facturaMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FacturacionResumenDto resumen() {
        List<Factura> todas = facturaRepository.findAll();
        BigDecimal ventas = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        long aceptadas = 0, rechazadas = 0, pendientes = 0;
        for (Factura f : todas) {
            switch (f.getEstado()) {
                case ACEPTADA_DIAN -> aceptadas++;
                case RECHAZADA -> rechazadas++;
                case PENDIENTE -> pendientes++;
            }
            if (f.getEstado() != EstadoFactura.RECHAZADA) {
                ventas = ventas.add(f.getTotal());
                iva = iva.add(f.getIva());
            }
        }
        return new FacturacionResumenDto(ventas, iva, todas.size(), aceptadas, rechazadas, pendientes);
    }

    /** Emite la factura de un pedido existente (POST /api/facturas). */
    @Transactional
    public FacturaDto generar(FacturaCreateRequest request) {
        return facturaMapper.toDto(emitir(pedidoService.buscar(request.pedidoId())));
    }

    /**
     * Lógica compartida con el cobro de mesa y el DataLoader: crea la factura, la valida, cierra el
     * pedido (PAGADO), libera su mesa y actualiza al cliente.
     */
    @Transactional
    public Factura emitir(Pedido pedido) {
        if (pedido.getFactura() != null) {
            throw new IllegalStateException("El pedido " + pedido.getNumeroPedido() + " ya tiene factura");
        }
        if (pedido.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("No se puede facturar un pedido sin ítems");
        }
        pedido.recalcularTotales();

        Factura factura = new Factura();
        factura.setPedido(pedido);
        factura.setCliente(pedido.getCliente());
        factura.setNumero(siguienteNumero(pedido.getCliente() != null));
        factura.generarFactura();
        factura.validarDIAN();
        facturaRepository.save(factura);
        pedido.setFactura(factura);

        // Facturar = cobrar: el pedido queda pagado y la mesa libre.
        if (!pedido.estaPagado()) {
            pedido.cambiarEstado(EstadoPedido.PAGADO);
            if (pedido.getMesa() != null) {
                pedido.getMesa().liberar();
            }
        }

        Cliente cliente = pedido.getCliente();
        if (cliente != null) {
            // Factura.cliente es el lado dueño; la colección inversa solo se mantiene al día en memoria.
            // contains() inicializa el bag si estaba perezoso (la factura ya está insertada y entra
            // en la carga), así que solo se agrega cuando la colección ya estaba cargada sin ella.
            // Un add() directo sobre un bag perezoso se encola y duplicaría la factura al inicializarse.
            if (!cliente.getFacturas().contains(factura)) {
                cliente.getFacturas().add(factura);
            }
            cliente.incrementarVisitas();
            cliente.calcularTotalGastado();
        }
        return factura;
    }

    @Transactional
    public FacturaDto validarDian(Long id) {
        Factura factura = buscar(id);
        factura.validarDIAN();
        return facturaMapper.toDto(factura);
    }

    @Transactional
    public FacturaDto anular(Long id) {
        Factura factura = buscar(id);
        factura.anularFactura();
        if (factura.getCliente() != null) {
            factura.getCliente().calcularTotalGastado();
        }
        return facturaMapper.toDto(factura);
    }

    private Factura buscar(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + id));
    }

    /** FE-#### para clientes identificados, POS-#### para consumidor final. */
    private String siguienteNumero(boolean electronica) {
        String prefijo = electronica ? "FE-" : "POS-";
        long n = facturaRepository.count() + 2101;
        while (facturaRepository.existsByNumero(prefijo + n)) {
            n++;
        }
        return prefijo + n;
    }
}
