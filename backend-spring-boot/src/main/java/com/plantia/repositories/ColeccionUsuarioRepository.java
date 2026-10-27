package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.ColeccionUsuario;

public interface ColeccionUsuarioRepository extends JpaRepository<ColeccionUsuario, Integer> {

    List<ColeccionUsuario> findByIdUsuarioOrderByUltimaVezDesc(Integer idUsuario);

    Optional<ColeccionUsuario> findByIdUsuarioAndIdCatalogo(Integer idUsuario, Integer idCatalogo);

    long countByIdUsuario(Integer idUsuario);
}
