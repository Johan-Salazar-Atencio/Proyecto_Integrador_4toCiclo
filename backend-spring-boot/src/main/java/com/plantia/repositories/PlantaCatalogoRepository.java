package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.plantia.models.PlantaCatalogo;

public interface PlantaCatalogoRepository
        extends JpaRepository<PlantaCatalogo, Integer>, JpaSpecificationExecutor<PlantaCatalogo> {

    Optional<PlantaCatalogo> findByNombreCientificoIgnoreCase(String nombreCientifico);

    /** La primera especie con ese nombre sin distinguir mayusculas (si el admin dejo dos, no revienta con 500). */
    Optional<PlantaCatalogo> findFirstByNombreCientificoIgnoreCaseOrderByIdCatalogoAsc(String nombreCientifico);

    /**
     * Candado de la transaccion por nombre de especie (en minusculas): dos escaneos simultaneos de la misma especie
     * nueva van uno detras del otro y el segundo ve la fila que creo el primero (sin choque con el UNIQUE).
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(7101, hashtext(:nombre))) candado", nativeQuery = true)
    Integer bloquearEspecie(@org.springframework.data.repository.query.Param("nombre") String nombreEnMinusculas);
}
