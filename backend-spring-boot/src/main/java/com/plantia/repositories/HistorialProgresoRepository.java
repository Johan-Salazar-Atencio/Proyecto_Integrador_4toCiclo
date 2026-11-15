package com.plantia.repositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.HistorialProgreso;

public interface HistorialProgresoRepository extends JpaRepository<HistorialProgreso, Integer> {

    /** Cuántas veces ganó XP el usuario con una regla desde el momento indicado (tope diario). */
    long countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(Integer idUsuario, Integer idRegla, LocalDateTime desde);
}
