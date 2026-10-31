package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.plantia.models.PlantaCatalogo;

public interface PlantaCatalogoRepository
        extends JpaRepository<PlantaCatalogo, Integer>, JpaSpecificationExecutor<PlantaCatalogo> {

    Optional<PlantaCatalogo> findByNombreCientificoIgnoreCase(String nombreCientifico);
}
