package com.university.demo.application.service;

import com.university.demo.application.dto.IngredienteDto;
import com.university.demo.application.dto.IngredienteRequest;
import com.university.demo.application.dto.MovimientoDto;
import com.university.demo.application.dto.MovimientoRequest;
import com.university.demo.application.mapper.InventarioMapper;
import com.university.demo.domain.EstadoStock;
import com.university.demo.domain.Ingrediente;
import com.university.demo.domain.Inventario;
import com.university.demo.domain.MovimientoInventario;
import com.university.demo.domain.TipoMovimiento;
import com.university.demo.infrastructure.repository.IngredienteRepository;
import com.university.demo.infrastructure.repository.InventarioRepository;
import com.university.demo.infrastructure.repository.MovimientoInventarioRepository;
import com.university.demo.infrastructure.repository.ProveedorRepository;
import com.university.demo.presentation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Casos de uso de Inventario / Ingrediente / MovimientoInventario. */
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final IngredienteRepository ingredienteRepository;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProveedorRepository proveedorRepository;
    private final InventarioMapper mapper;

    /** consultarStock() del diagrama. */
    @Transactional(readOnly = true)
    public List<IngredienteDto> consultarStock() {
        return ingredienteRepository.findAll().stream().map(mapper::toDto).toList();
    }

    /** verificarStockMinimo(): ingredientes en estado BAJO o CRITICO. */
    @Transactional(readOnly = true)
    public List<IngredienteDto> alertas() {
        return ingredienteRepository.findByEstadoIn(List.of(EstadoStock.BAJO, EstadoStock.CRITICO))
                .stream().map(mapper::toDto).toList();
    }

    /** agregarIngrediente() del diagrama. */
    @Transactional
    public IngredienteDto agregarIngrediente(IngredienteRequest r) {
        Inventario inventario = inventarioRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay inventario configurado"));
        Ingrediente ing = new Ingrediente();
        ing.setNombre(r.nombre());
        ing.setStock(r.stock());
        ing.setUnidad(r.unidad());
        ing.setStockMinimo(r.stockMinimo());
        ing.setCostoUnitario(r.costoUnitario() != null ? r.costoUnitario() : BigDecimal.ZERO);
        if (r.proveedorId() != null) {
            ing.setProveedor(proveedorRepository.findById(r.proveedorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + r.proveedorId())));
        }
        ing.recalcularEstado();
        inventario.agregarIngrediente(ing);
        // Ingrediente es nuevo: persist directo para que ESTA instancia reciba su id
        // (saveAndFlush sobre el Inventario gestionado haría merge y devolvería id = null).
        ingredienteRepository.saveAndFlush(ing);
        return mapper.toDto(ing);
    }

    /** registrarMovimiento(): ENTRADA suma, MERMA resta, AJUSTE fija el stock. Guarda el historial. */
    @Transactional
    public MovimientoDto registrarMovimiento(Long ingredienteId, MovimientoRequest r) {
        Ingrediente ing = buscar(ingredienteId);
        if (r.tipo() != TipoMovimiento.AJUSTE && r.cantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        MovimientoInventario mov = new MovimientoInventario();
        mov.setIngrediente(ing);
        mov.setTipo(r.tipo());
        mov.setCantidad(r.cantidad());
        mov.setObservaciones(r.observaciones());
        mov.setCostoUnitario(r.costoUnitario() != null ? r.costoUnitario() : ing.getCostoUnitario());

        ing.getInventario().registrarMovimiento(mov);   // valida stock y recalcula el estado

        if (r.tipo() == TipoMovimiento.ENTRADA && r.costoUnitario() != null) {
            ing.setCostoUnitario(r.costoUnitario());
        }
        return mapper.toDto(movimientoRepository.save(mov));
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> movimientos(Long ingredienteId) {
        buscar(ingredienteId);
        return movimientoRepository.findByIngredienteIdOrderByFechaDesc(ingredienteId)
                .stream().map(mapper::toDto).toList();
    }

    private Ingrediente buscar(Long id) {
        return ingredienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ingrediente no encontrado: " + id));
    }
}
