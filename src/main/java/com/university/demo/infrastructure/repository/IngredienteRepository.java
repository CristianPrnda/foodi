package com.university.demo.infrastructure.repository;

import com.university.demo.domain.EstadoStock;
import com.university.demo.domain.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {

    List<Ingrediente> findByEstado(EstadoStock estado);

    List<Ingrediente> findByEstadoIn(Collection<EstadoStock> estados);
}
