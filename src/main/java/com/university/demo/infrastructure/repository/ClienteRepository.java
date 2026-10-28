package com.university.demo.infrastructure.repository;

import com.university.demo.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByNombreContainingIgnoreCaseOrDocumentoContainingIgnoreCase(String nombre, String documento);

    boolean existsByDocumento(String documento);

    /** Para validar al actualizar: otro cliente (distinto del que se edita) con el mismo documento. */
    boolean existsByDocumentoAndIdNot(String documento, Long id);
}
