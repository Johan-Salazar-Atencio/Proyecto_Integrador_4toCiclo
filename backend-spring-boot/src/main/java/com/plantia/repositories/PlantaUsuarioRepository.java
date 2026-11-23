package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.PlantaUsuario;

public interface PlantaUsuarioRepository extends JpaRepository<PlantaUsuario, Integer> {

    List<PlantaUsuario> findByIdUsuarioOrderByFechaPlantadoDesc(Integer idUsuario);

    /** Plantas de los usuarios cuya region coincide (por su codigo, igual que la alerta del Home). */
    @Query("SELECT p FROM PlantaUsuario p WHERE p.idUsuario IN "
            + "(SELECT u.idUsuario FROM Usuario u LEFT JOIN u.regionEntidad r WHERE COALESCE(r.codigo, 'GLOBAL') = :region)")
    List<PlantaUsuario> findDeUsuariosDeRegion(@Param("region") String region);

    /**
     * Bloquea (SELECT ... FOR UPDATE) la fila de la planta hasta el fin de la transaccion. Se llama ANTES de leerla en
     * todo camino que la lee, la cambia y la guarda: asi un GET que la envejece o un cuidado simultaneo no guardan
     * valores viejos encima de otro (QA-03). Orden de candados: primero el usuario (si hace falta), luego plantas.
     */
    @Query(value = "SELECT id_planta_usuario FROM plantas_usuario WHERE id_planta_usuario = :id FOR UPDATE", nativeQuery = true)
    Integer bloquear(@Param("id") Integer idPlantaUsuario);

    /** Bloquea todas las plantas del usuario, siempre en orden de id (dos transacciones nunca se cruzan). */
    @Query(value = "SELECT id_planta_usuario FROM plantas_usuario WHERE id_usuario = :u ORDER BY id_planta_usuario FOR UPDATE",
            nativeQuery = true)
    List<Integer> bloquearDeUsuario(@Param("u") Integer idUsuario);

    Optional<PlantaUsuario> findFirstByIdUsuarioAndEtapaCrecimientoNotOrderByFechaPlantadoDesc(
            Integer idUsuario, String etapa);
}
