package com.university.demo.application.service;

import com.university.demo.application.dto.*;
import com.university.demo.application.mapper.PedidoMapper;
import com.university.demo.domain.Cliente;
import com.university.demo.domain.DetallePedido;
import com.university.demo.domain.EstadoMesa;
import com.university.demo.domain.EstadoPedido;
import com.university.demo.domain.Mesa;
import com.university.demo.domain.Pedido;
import com.university.demo.infrastructure.repository.ClienteRepository;
import com.university.demo.infrastructure.repository.MesaRepository;
import com.university.demo.infrastructure.repository.PedidoRepository;
import com.university.demo.presentation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso de Pedido / DetallePedido (abrir cuenta, agregar ítems, cambiar estado). */
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public List<PedidoDto> listarRecientes() {
        return pedidoRepository.findTop50ByOrderByFechaDesc().stream().map(pedidoMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDto obtener(Long id) {
        return pedidoMapper.toDto(buscar(id));
    }

    /** Abre la cuenta de una mesa. Si la mesa estaba libre pasa a OCUPADA. */
    @Transactional
    public PedidoDto abrir(PedidoCreateRequest request) {
        Mesa mesa = mesaRepository.findById(request.mesaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada: " + request.mesaId()));
        if (pedidoRepository.findFirstByMesaIdAndEstadoNotOrderByFechaDesc(mesa.getId(), EstadoPedido.PAGADO).isPresent()) {
            throw new IllegalStateException("La mesa " + mesa.getNumero() + " ya tiene un pedido abierto");
        }
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(siguienteNumero());
        pedido.setMesero(request.mesero());
        pedido.setMesa(mesa);
        if (request.clienteId() != null) {
            pedido.setCliente(buscarCliente(request.clienteId()));
        }
        if (mesa.getEstado() == EstadoMesa.LIBRE) {
            mesa.ocupar();
        }
        return pedidoMapper.toDto(pedidoRepository.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoDto agregarDetalle(Long pedidoId, DetalleRequest request) {
        Pedido pedido = buscarAbierto(pedidoId);
        DetallePedido detalle = new DetallePedido();
        detalle.setDescripcion(request.descripcion());
        detalle.setCantidad(request.cantidad());
        detalle.setPrecioUnitario(request.precioUnitario());
        detalle.calcularSubtotal();
        pedido.agregarDetalle(detalle);
        pedidoRepository.flush();   // el pedido ya es gestionado: el cascade persiste el detalle y le asigna id
        return pedidoMapper.toDto(pedido);
    }

    @Transactional
    public PedidoDto modificarCantidad(Long pedidoId, Long detalleId, CantidadRequest request) {
        Pedido pedido = buscarAbierto(pedidoId);
        buscarDetalle(pedido, detalleId).modificarCantidad(request.cantidad());
        pedidoRepository.flush();
        return pedidoMapper.toDto(pedido);
    }

    @Transactional
    public PedidoDto eliminarDetalle(Long pedidoId, Long detalleId) {
        Pedido pedido = buscarAbierto(pedidoId);
        pedido.eliminarDetalle(buscarDetalle(pedido, detalleId));
        pedidoRepository.flush();
        return pedidoMapper.toDto(pedido);
    }

    @Transactional
    public PedidoDto cambiarEstado(Long pedidoId, CambiarEstadoRequest request) {
        EstadoPedido nuevo = parsearEstado(request.estado());
        if (nuevo == EstadoPedido.PAGADO) {
            throw new IllegalArgumentException("Para pagar un pedido usa el cobro de la mesa");
        }
        Pedido pedido = buscarAbierto(pedidoId);
        pedido.cambiarEstado(nuevo);
        return pedidoMapper.toDto(pedido);
    }

    // ---------- helpers (los usa también MesaService / FacturaService) ----------

    Pedido buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    private Pedido buscarAbierto(Long id) {
        Pedido pedido = buscar(id);
        if (pedido.estaPagado()) {
            throw new IllegalStateException("El pedido " + pedido.getNumeroPedido() + " ya está pagado");
        }
        return pedido;
    }

    private DetallePedido buscarDetalle(Pedido pedido, Long detalleId) {
        return pedido.getDetalles().stream()
                .filter(d -> detalleId.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado: " + detalleId));
    }

    private EstadoPedido parsearEstado(String texto) {
        try {
            return EstadoPedido.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado inválido: " + texto + ". Válidos: ABIERTO, EN_COCINA, SERVIDO");
        }
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    private String siguienteNumero() {
        long n = pedidoRepository.count() + 1001;
        while (pedidoRepository.existsByNumeroPedido("PED-" + n)) {
            n++;
        }
        return "PED-" + n;
    }
}
