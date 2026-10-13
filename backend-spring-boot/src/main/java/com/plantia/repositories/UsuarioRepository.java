package com.plantia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
}
