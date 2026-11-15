package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.PlantaUsuario;

public interface PlantaUsuarioRepository extends JpaRepository<PlantaUsuario, Integer> {

    List<PlantaUsuario> findByIdUsuarioOrderByFechaPlantadoDesc(Integer idUsuario);

    /** Plantas de los usuarios cuya region coincide (texto, igual que la alerta del Home). */
    @Query("SELECT p FROM PlantaUsuario p WHERE p.idUsuario IN "
            + "(SELECT u.idUsuario FROM Usuario u WHERE u.region = :region)")
    List<PlantaUsuario> findDeUsuariosDeRegion(@Param("region") String region);

    Optional<PlantaUsuario> findFirstByIdUsuarioAndEtapaCrecimientoNotOrderByFechaPlantadoDesc(
            Integer idUsuario, String etapa);
}
