package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.EscaneoHistorial;

public interface EscaneoHistorialRepository extends JpaRepository<EscaneoHistorial, Integer> {

    List<EscaneoHistorial> findTop20ByIdUsuarioOrderByFechaEscaneoDesc(Integer idUsuario);

    /** La foto mas reciente que el usuario tomo de una especie (para mostrarla en su coleccion). */
    java.util.Optional<EscaneoHistorial> findFirstByIdUsuarioAndIdCatalogoOrderByFechaEscaneoDesc(Integer idUsuario, Integer idCatalogo);

    /** Para saber si una especie es nueva para el usuario. */
    boolean existsByIdUsuarioAndIdCatalogo(Integer idUsuario, Integer idCatalogo);

    long countByIdUsuarioAndIdCatalogoIsNotNull(Integer idUsuario);

    @Query("SELECT COUNT(DISTINCT e.idCatalogo) FROM EscaneoHistorial e WHERE e.idUsuario = :idUsuario AND e.idCatalogo IS NOT NULL")
    long countDistinctEspeciesByIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query(value = "SELECT COUNT(*) FROM escaneos_historial e WHERE e.id_usuario = :idUsuario AND e.id_catalogo IS NOT NULL "
            + "AND EXTRACT(HOUR FROM e.fecha_escaneo) BETWEEN :desde AND :hasta", nativeQuery = true)
    long countEscaneosEnFranja(@Param("idUsuario") Integer idUsuario, @Param("desde") int desde, @Param("hasta") int hasta);
}
