package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.LogroUsuario;

public interface LogroUsuarioRepository extends JpaRepository<LogroUsuario, Integer> {

    /**
     * Crea la fila de avance de cada logro activo que el usuario aun no tiene. ON CONFLICT DO NOTHING:
     * si otra transaccion la creo antes, no falla por la clave unica (id_usuario, id_logro).
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO logros_usuario (id_usuario, id_logro, progreso, notificado) "
            + "SELECT :idUsuario, l.id_logro, 0, FALSE FROM logros l WHERE l.activo "
            + "ON CONFLICT (id_usuario, id_logro) DO NOTHING", nativeQuery = true)
    int crearFaltantes(@Param("idUsuario") Integer idUsuario);

    /** Marca como vistos, en una sola sentencia, los logros ya desbloqueados de ESE usuario (nunca de otro). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE logros_usuario SET notificado = TRUE WHERE id_usuario = :idUsuario AND id_logro IN (:ids) "
            + "AND desbloqueado_en IS NOT NULL AND notificado = FALSE", nativeQuery = true)
    int marcarVistos(@Param("idUsuario") Integer idUsuario, @Param("ids") java.util.Collection<Integer> ids);

    List<LogroUsuario> findByIdUsuario(Integer idUsuario);

    Optional<LogroUsuario> findByIdUsuarioAndIdLogro(Integer idUsuario, Integer idLogro);

    List<LogroUsuario> findByIdUsuarioAndDesbloqueadoEnIsNotNullAndNotificadoFalse(Integer idUsuario);
}
