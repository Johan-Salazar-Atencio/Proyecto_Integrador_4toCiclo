package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.ColeccionUsuario;

public interface ColeccionUsuarioRepository extends JpaRepository<ColeccionUsuario, Integer> {

    List<ColeccionUsuario> findByIdUsuarioOrderByUltimaVezDesc(Integer idUsuario);

    Optional<ColeccionUsuario> findByIdUsuarioAndIdCatalogo(Integer idUsuario, Integer idCatalogo);

    long countByIdUsuario(Integer idUsuario);

    /** Bloquea al usuario (FOR UPDATE): dos "guardar en la coleccion" simultaneos van uno detras del otro (sin 409). */
    @org.springframework.data.jpa.repository.Query(value = "SELECT id_usuario FROM usuarios WHERE id_usuario = :u FOR UPDATE", nativeQuery = true)
    Integer bloquearUsuario(@org.springframework.data.repository.query.Param("u") Integer idUsuario);
}
