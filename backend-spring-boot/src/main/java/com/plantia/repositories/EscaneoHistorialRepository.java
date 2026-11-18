package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.EscaneoHistorial;

public interface EscaneoHistorialRepository extends JpaRepository<EscaneoHistorial, Integer> {

    List<EscaneoHistorial> findTop20ByIdUsuarioOrderByFechaEscaneoDesc(Integer idUsuario);

    /** La foto mas reciente que el usuario tomo de una especie (para mostrarla en su coleccion). */
    java.util.Optional<EscaneoHistorial> findFirstByIdUsuarioAndIdCatalogoOrderByFechaEscaneoDesc(Integer idUsuario, Integer idCatalogo);

    /** Para saber si una especie es nueva para el usuario. */
    boolean existsByIdUsuarioAndIdCatalogo(Integer idUsuario, Integer idCatalogo);
}
