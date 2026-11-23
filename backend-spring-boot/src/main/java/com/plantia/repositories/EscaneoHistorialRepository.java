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

    /** [id_catalogo, foto] del escaneo mas reciente del usuario por especie, en una sola consulta (QA-23). */
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT ON (id_catalogo) id_catalogo, imagen_foto_url "
            + "FROM escaneos_historial WHERE id_usuario = :u AND id_catalogo IN (:ids) ORDER BY id_catalogo, fecha_escaneo DESC",
            nativeQuery = true)
    java.util.List<Object[]> ultimasFotos(@org.springframework.data.repository.query.Param("u") Integer idUsuario,
                                          @org.springframework.data.repository.query.Param("ids") java.util.Collection<Integer> idsCatalogo);

    long countByIdUsuarioAndIdCatalogoIsNotNull(Integer idUsuario);

    @Query("SELECT COUNT(DISTINCT e.idCatalogo) FROM EscaneoHistorial e WHERE e.idUsuario = :idUsuario AND e.idCatalogo IS NOT NULL")
    long countDistinctEspeciesByIdUsuario(@Param("idUsuario") Integer idUsuario);

    /**
     * Escaneos identificados cuya hora, vista en {@code zonaUsuario}, cae entre {@code desde} y {@code hasta}.
     * fecha_escaneo es TIMESTAMP sin zona escrito con LocalDateTime.now() del servidor, asi que primero se
     * interpreta en {@code zonaServidor} y luego se pasa a la zona del jugador.
     */
    @Query(value = "SELECT COUNT(*) FROM escaneos_historial e WHERE e.id_usuario = :idUsuario AND e.id_catalogo IS NOT NULL "
            + "AND EXTRACT(HOUR FROM ((e.fecha_escaneo AT TIME ZONE CAST(:zonaServidor AS TEXT)) "
            + "AT TIME ZONE CAST(:zonaUsuario AS TEXT))) BETWEEN :desde AND :hasta", nativeQuery = true)
    long countEscaneosEnFranja(@Param("idUsuario") Integer idUsuario, @Param("desde") int desde, @Param("hasta") int hasta,
                               @Param("zonaServidor") String zonaServidor, @Param("zonaUsuario") String zonaUsuario);
}
