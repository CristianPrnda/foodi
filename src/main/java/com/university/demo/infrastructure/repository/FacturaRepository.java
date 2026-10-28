package com.university.demo.infrastructure.repository;

import com.university.demo.domain.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findAllByOrderByFechaDesc();

    List<Factura> findTop5ByOrderByFechaDesc();

    boolean existsByNumero(String numero);
}
