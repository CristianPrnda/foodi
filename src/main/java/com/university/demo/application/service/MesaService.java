package com.university.demo.application.service;

import com.university.demo.application.dto.CobroRequest;
import com.university.demo.application.dto.FacturaDto;
import com.university.demo.application.dto.MesaCreateRequest;
import com.university.demo.application.dto.MesaDto;
import com.university.demo.application.mapper.FacturaMapper;
import com.university.demo.application.mapper.MesaMapper;
import com.university.demo.domain.EstadoMesa;
import com.university.demo.domain.EstadoPedido;
import com.university.demo.domain.Factura;
import com.university.demo.domain.Mesa;
import com.university.demo.domain.Pedido;
import com.university.demo.domain.Restaurante;
import com.university.demo.infrastructure.repository.ClienteRepository;
import com.university.demo.infrastructure.repository.MesaRepository;
import com.university.demo.infrastructure.repository.PedidoRepository;
import com.university.demo.infrastructure.repository.RestauranteRepository;
import com.university.demo.presentation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Casos de uso de Mesa: ocupar, liberar, solicitar pago y cobrar (cierra el pedido y factura). */
@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;
    private final RestauranteRepository restauranteRepository;
    private final ClienteRepository clienteRepository;
    private final FacturaService facturaService;
    private final MesaMapper mesaMapper;
    private final FacturaMapper facturaMapper;

    @Transactional(readOnly = true)
    public List<MesaDto> listar() {
        return mesaRepository.findAllByOrderByNumeroAsc().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public MesaDto obtener(Long id) {
        return aDto(buscar(id));
    }

    /** registrarMesa() del diagrama: agrega una mesa a la (única) sede. */
    @Transactional
    public MesaDto crear(MesaCreateRequest request) {
        Restaurante restaurante = restauranteRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay restaurante configurado"));
        if (mesaRepository.existsByRestauranteIdAndNumero(restaurante.getId(), request.numero())) {
            throw new IllegalArgumentException("Ya existe la mesa " + request.numero());
        }
        Mesa mesa = new Mesa();
        mesa.setNumero(request.numero());
        mesa.setRestaurante(restaurante);
        // Mesa es nueva: save() hace persist y la misma instancia queda con su id.
        // (saveAndFlush sobre el Restaurante gestionado haría merge y dejaría esta instancia sin id.)
        mesaRepository.saveAndFlush(mesa);
        return aDto(mesa);
    }

    @Transactional
    public MesaDto ocupar(Long id) {
        Mesa mesa = buscar(id);
        if (mesa.getEstado() != EstadoMesa.LIBRE) {
            throw new IllegalStateException("La mesa " + mesa.getNumero() + " no está libre");
        }
        mesa.ocupar();
        return aDto(mesa);
    }

    @Transactional
    public MesaDto solicitarPago(Long id) {
        Mesa mesa = buscar(id);
        if (mesa.getEstado() == EstadoMesa.LIBRE) {
            throw new IllegalStateException("La mesa " + mesa.getNumero() + " está libre");
        }
        mesa.solicitarPago();
        return aDto(mesa);
    }

    /** Libera una mesa sin cobrar. Solo si no tiene una cuenta con ítems pendientes. */
    @Transactional
    public MesaDto liberar(Long id) {
        Mesa mesa = buscar(id);
        Optional<Pedido> activo = pedidoActivo(mesa);
        if (activo.isPresent() && !activo.get().getDetalles().isEmpty()) {
            throw new IllegalStateException("La mesa " + mesa.getNumero() + " tiene una cuenta pendiente: cóbrala primero");
        }
        activo.ifPresent(pedidoRepository::delete);
        mesa.liberar();
        return aDto(mesa);
    }

    /** Cobra la cuenta: pedido PAGADO + factura emitida y validada + mesa libre. */
    @Transactional
    public FacturaDto cobrar(Long id, CobroRequest request) {
        Mesa mesa = buscar(id);
        Pedido pedido = pedidoActivo(mesa)
                .orElseThrow(() -> new IllegalStateException("La mesa " + mesa.getNumero() + " no tiene cuenta abierta"));
        if (request != null && request.clienteId() != null) {
            pedido.setCliente(clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + request.clienteId())));
        }
        // emitir() ya cierra el pedido (PAGADO) y libera la mesa.
        Factura factura = facturaService.emitir(pedido);
        mesa.liberar();
        return facturaMapper.toDto(factura);
    }

    private Optional<Pedido> pedidoActivo(Mesa mesa) {
        return pedidoRepository.findFirstByMesaIdAndEstadoNotOrderByFechaDesc(mesa.getId(), EstadoPedido.PAGADO);
    }

    private MesaDto aDto(Mesa mesa) {
        return mesaMapper.toDto(mesa, pedidoActivo(mesa).orElse(null));
    }

    private Mesa buscar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada: " + id));
    }
}
