package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.EventoDesastre;

public interface EventoDesastreRepository extends JpaRepository<EventoDesastre, Integer> {

    @Query("""
            SELECT e FROM EventoDesastre e
            WHERE e.estado = 'ACTIVO'
              AND e.fecha_inicio <= CURRENT_TIMESTAMP
              AND e.fecha_fin > CURRENT_TIMESTAMP
              AND (e.region = 'GLOBAL' OR e.region = :region)
            ORDER BY e.fecha_inicio DESC
            """)
    List<EventoDesastre> activosParaRegion(@Param("region") String region);
}
