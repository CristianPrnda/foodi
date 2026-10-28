package com.university.demo.infrastructure.repository;

import com.university.demo.domain.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
}
