package com.university.demo.application.service;

import com.university.demo.application.dto.ClienteDto;
import com.university.demo.application.dto.ClienteRequest;
import com.university.demo.application.dto.PedidoDto;
import com.university.demo.application.mapper.ClienteMapper;
import com.university.demo.application.mapper.PedidoMapper;
import com.university.demo.domain.Cliente;
import com.university.demo.infrastructure.repository.ClienteRepository;
import com.university.demo.infrastructure.repository.PedidoRepository;
import com.university.demo.presentation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso de Cliente: registrarCliente, actualizarDatos, consultarHistorial, calcularTotalGastado. */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteMapper clienteMapper;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public List<ClienteDto> listar(String busqueda) {
        List<Cliente> clientes = (busqueda == null || busqueda.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByNombreContainingIgnoreCaseOrDocumentoContainingIgnoreCase(busqueda.trim(), busqueda.trim());
        return clientes.stream().map(clienteMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ClienteDto obtener(Long id) {
        return clienteMapper.toDto(buscar(id));
    }

    @Transactional
    public ClienteDto registrar(ClienteRequest request) {
        String documento = ClienteMapper.normalizar(request.documento());
        if (documento != null && clienteRepository.existsByDocumento(documento)) {
            throw new IllegalArgumentException("Ya existe un cliente con ese documento");
        }
        return clienteMapper.toDto(clienteRepository.save(clienteMapper.toEntity(request)));
    }

    @Transactional
    public ClienteDto actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        String documento = ClienteMapper.normalizar(request.documento());
        if (documento != null && clienteRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new IllegalArgumentException("Ya existe otro cliente con ese documento");
        }
        cliente.actualizarDatos(request.nombre(), request.email(), request.telefono(), request.direccion());
        if (documento != null) {
            cliente.setDocumento(documento);
        }
        return clienteMapper.toDto(cliente);
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> historial(Long id) {
        buscar(id);
        return pedidoRepository.findByClienteIdOrderByFechaDesc(id).stream().map(pedidoMapper::toDto).toList();
    }

    /** Recalcula totalGastado a partir de sus facturas no rechazadas. */
    @Transactional
    public ClienteDto recalcularTotal(Long id) {
        Cliente cliente = buscar(id);
        cliente.calcularTotalGastado();
        return clienteMapper.toDto(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }
}
