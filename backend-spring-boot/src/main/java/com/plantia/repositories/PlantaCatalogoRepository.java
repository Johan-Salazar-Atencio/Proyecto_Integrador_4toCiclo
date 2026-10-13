package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.PlantaCatalogo;

public interface PlantaCatalogoRepository extends JpaRepository<PlantaCatalogo, Integer> {

    Optional<PlantaCatalogo> findByNombreCientificoIgnoreCase(String nombreCientifico);

    @Query("""
            SELECT p FROM PlantaCatalogo p
            WHERE (:q IS NULL OR LOWER(p.nombreComun) LIKE LOWER(CONCAT('%', :q, '%'))
                                OR LOWER(p.nombreCientifico) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:toxica IS NULL OR p.esToxicaMascotas = :toxica)
            ORDER BY p.nombreComun
            """)
    List<PlantaCatalogo> buscar(@Param("q") String q, @Param("toxica") Boolean toxica);
}
