package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.TemaFondo;

public interface TemaFondoRepository extends JpaRepository<TemaFondo, Integer> {

    Optional<TemaFondo> findByEsPredeterminadoTrue();
}
