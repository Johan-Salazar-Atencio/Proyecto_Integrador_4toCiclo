package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.LogroUsuario;

public interface LogroUsuarioRepository extends JpaRepository<LogroUsuario, Integer> {

    List<LogroUsuario> findByIdUsuario(Integer idUsuario);

    Optional<LogroUsuario> findByIdUsuarioAndIdLogro(Integer idUsuario, Integer idLogro);

    List<LogroUsuario> findByIdUsuarioAndDesbloqueadoEnIsNotNullAndNotificadoFalse(Integer idUsuario);
}
