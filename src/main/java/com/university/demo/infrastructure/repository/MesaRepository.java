package com.university.demo.infrastructure.repository;

import com.university.demo.domain.EstadoMesa;
import com.university.demo.domain.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    long countByEstado(EstadoMesa estado);

    List<Mesa> findAllByOrderByNumeroAsc();

    boolean existsByRestauranteIdAndNumero(Long restauranteId, int numero);
}
