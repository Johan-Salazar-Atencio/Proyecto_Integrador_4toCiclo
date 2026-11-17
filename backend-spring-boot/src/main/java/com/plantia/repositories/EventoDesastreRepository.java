package com.plantia.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.EventoDesastre;

public interface EventoDesastreRepository extends JpaRepository<EventoDesastre, Integer> {

    @Query("""
            SELECT e FROM EventoDesastre e LEFT JOIN e.regionEntidad r
            WHERE e.estado = 'ACTIVO'
              AND e.fechaInicio <= CURRENT_TIMESTAMP
              AND e.fechaFin > CURRENT_TIMESTAMP
              AND (r IS NULL OR r.codigo = 'GLOBAL' OR r.codigo = :region)
            ORDER BY e.fechaInicio DESC
            """)
    List<EventoDesastre> activosParaRegion(@Param("region") String region);

    List<EventoDesastre> findByEstadoAndFechaInicioLessThanEqual(String estado, LocalDateTime limite);

    List<EventoDesastre> findByEstadoAndFechaFinLessThanEqual(String estado, LocalDateTime limite);

    List<EventoDesastre> findByEstadoAndProcesado(String estado, Boolean procesado);
}
