package com.plantia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.RegistroDesastreUsuario;

public interface RegistroDesastreUsuarioRepository extends JpaRepository<RegistroDesastreUsuario, Integer> {

    boolean existsByIdDesastreAndIdPlantaUsuario(Integer idDesastre, Integer idPlantaUsuario);
}
