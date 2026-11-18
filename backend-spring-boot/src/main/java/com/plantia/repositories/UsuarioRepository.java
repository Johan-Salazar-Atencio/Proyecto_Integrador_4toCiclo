package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Bloquea la fila del usuario hasta que termine la transaccion (SELECT ... FOR UPDATE).
     * Se llama al EMPEZAR toda operacion que cambia sus monedas o su XP, antes de leerlo: asi dos
     * compras o premios simultaneos van uno detras del otro y ninguno trabaja con un saldo viejo.
     */
    @Query(value = "SELECT id_usuario FROM usuarios WHERE id_usuario = :id FOR UPDATE", nativeQuery = true)
    Integer bloquear(@Param("id") Integer id);
}
