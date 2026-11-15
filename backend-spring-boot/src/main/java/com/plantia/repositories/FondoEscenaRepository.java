package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.FondoEscena;

public interface FondoEscenaRepository extends JpaRepository<FondoEscena, Integer> {

    List<FondoEscena> findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc();

    /** Escenas activas de esos temas. */
    List<FondoEscena> findByIdTemaInAndActivoTrue(java.util.Collection<Integer> idsTema);
}
