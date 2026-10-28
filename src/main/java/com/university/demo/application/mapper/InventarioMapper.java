package com.university.demo.application.mapper;

import com.university.demo.application.dto.IngredienteDto;
import com.university.demo.application.dto.MovimientoDto;
import com.university.demo.domain.Ingrediente;
import com.university.demo.domain.MovimientoInventario;
import org.springframework.stereotype.Component;

@Component
public class InventarioMapper {

    public IngredienteDto toDto(Ingrediente i) {
        return new IngredienteDto(i.getId(), i.getNombre(), i.getStock(), i.getUnidad(), i.getStockMinimo(),
                i.getCostoUnitario(), i.getEstado().name(),
                i.getProveedor() != null ? i.getProveedor().getId() : null,
                i.getProveedor() != null ? i.getProveedor().getNombre() : null);
    }

    public MovimientoDto toDto(MovimientoInventario m) {
        return new MovimientoDto(m.getId(), m.getTipo(), m.getCantidad(), m.getCostoUnitario(),
                m.getFecha(), m.getObservaciones(), m.getIngrediente().getNombre());
    }
}
