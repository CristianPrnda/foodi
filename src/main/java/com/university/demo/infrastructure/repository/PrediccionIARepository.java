package com.university.demo.infrastructure.repository;

import com.university.demo.domain.PrediccionIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PrediccionIARepository extends JpaRepository<PrediccionIA, Long> {

    Optional<PrediccionIA> findFirstByOrderByFechaDesc();

    Optional<PrediccionIA> findByFecha(LocalDate fecha);
}
