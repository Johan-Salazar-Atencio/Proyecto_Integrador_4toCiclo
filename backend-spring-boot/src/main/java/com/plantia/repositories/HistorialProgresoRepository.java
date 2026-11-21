package com.plantia.repositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.HistorialProgreso;

public interface HistorialProgresoRepository extends JpaRepository<HistorialProgreso, Integer> {

    /** Cuántas veces ganó XP el usuario con una regla desde el momento indicado (tope diario). */
    long countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(Integer idUsuario, Integer idRegla, LocalDateTime desde);

    @Query("SELECT COUNT(h) FROM HistorialProgreso h, ReglaProgreso r "
            + "WHERE h.idRegla = r.idRegla AND h.idUsuario = :idUsuario AND r.motivo = :motivo")
    long countByUsuarioAndMotivo(@Param("idUsuario") Integer idUsuario, @Param("motivo") String motivo);
}
