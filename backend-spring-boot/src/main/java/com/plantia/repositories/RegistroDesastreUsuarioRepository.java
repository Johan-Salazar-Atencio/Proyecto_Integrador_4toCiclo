package com.plantia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.RegistroDesastreUsuario;

public interface RegistroDesastreUsuarioRepository extends JpaRepository<RegistroDesastreUsuario, Integer> {

    boolean existsByIdDesastreAndIdPlantaUsuario(Integer idDesastre, Integer idPlantaUsuario);

    @Query("SELECT COUNT(r) FROM RegistroDesastreUsuario r, PlantaUsuario p WHERE r.idPlantaUsuario = p.idPlantaUsuario AND p.idUsuario = :idUsuario")
    long countByUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT COUNT(r) FROM RegistroDesastreUsuario r, PlantaUsuario p, EventoDesastre e, TipoDesastre t "
            + "WHERE r.idPlantaUsuario = p.idPlantaUsuario AND p.idUsuario = :idUsuario "
            + "AND r.idDesastre = e.idDesastre AND e.tipoDesastre = t AND t.codigo = :tipo")
    long countByUsuarioAndTipo(@Param("idUsuario") Integer idUsuario, @Param("tipo") String tipo);

    @Query("SELECT COUNT(DISTINCT t.codigo) FROM RegistroDesastreUsuario r, PlantaUsuario p, EventoDesastre e, TipoDesastre t "
            + "WHERE r.idPlantaUsuario = p.idPlantaUsuario AND p.idUsuario = :idUsuario "
            + "AND r.idDesastre = e.idDesastre AND e.tipoDesastre = t")
    long countTiposDistintosByUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT COUNT(r) FROM RegistroDesastreUsuario r, PlantaUsuario p "
            + "WHERE r.idPlantaUsuario = p.idPlantaUsuario AND p.idUsuario = :idUsuario "
            + "AND r.estabaRefugiada = TRUE AND r.estadoFinal = 'SALVADA'")
    long countPlantasRefugiadasByUsuario(@Param("idUsuario") Integer idUsuario);

    @Query(value = "SELECT COUNT(*) FROM ("
            + "SELECT r.id_desastre FROM registros_desastre_usuario r "
            + "JOIN plantas_usuario p ON p.id_planta_usuario = r.id_planta_usuario "
            + "WHERE p.id_usuario = :idUsuario "
            + "GROUP BY r.id_desastre "
            + "HAVING SUM(CASE WHEN r.estado_final IN ('DANADA','DESTRUIDA') THEN 1 ELSE 0 END) = 0"
            + ") t", nativeQuery = true)
    long countEventosSinPerdidasByUsuario(@Param("idUsuario") Integer idUsuario);
}
