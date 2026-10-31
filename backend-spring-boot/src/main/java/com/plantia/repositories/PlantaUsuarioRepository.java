package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.PlantaUsuario;

public interface PlantaUsuarioRepository extends JpaRepository<PlantaUsuario, Integer> {

    List<PlantaUsuario> findByIdUsuarioOrderByFechaPlantadoDesc(Integer idUsuario);

    Optional<PlantaUsuario> findFirstByIdUsuarioAndEtapaCrecimientoNotOrderByFechaPlantadoDesc(
            Integer idUsuario, String etapa);
}
