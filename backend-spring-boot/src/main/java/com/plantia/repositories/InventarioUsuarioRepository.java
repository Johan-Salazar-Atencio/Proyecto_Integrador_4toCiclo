package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.InventarioUsuario;

public interface InventarioUsuarioRepository extends JpaRepository<InventarioUsuario, Integer> {

    List<InventarioUsuario> findByIdUsuario(Integer idUsuario);

    List<InventarioUsuario> findByIdUsuarioAndEstaEquipadoTrue(Integer idUsuario);
}
