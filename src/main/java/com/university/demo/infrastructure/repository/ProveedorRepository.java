package com.university.demo.infrastructure.repository;

import com.university.demo.domain.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
