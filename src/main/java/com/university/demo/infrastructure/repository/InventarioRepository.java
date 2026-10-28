package com.university.demo.infrastructure.repository;

import com.university.demo.domain.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
}
