package com.university.demo.infrastructure.repository;

import com.university.demo.domain.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByIngredienteIdOrderByFechaDesc(Long ingredienteId);
}
