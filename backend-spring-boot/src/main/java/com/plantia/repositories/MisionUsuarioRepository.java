package com.plantia.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.MisionUsuario;

public interface MisionUsuarioRepository extends JpaRepository<MisionUsuario, Integer> {

    List<MisionUsuario> findByIdUsuarioAndFecha(Integer idUsuario, LocalDate fecha);
}
