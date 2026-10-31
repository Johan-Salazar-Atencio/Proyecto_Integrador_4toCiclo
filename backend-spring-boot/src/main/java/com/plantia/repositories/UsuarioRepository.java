package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
