package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.AbonoUsuario;

public interface AbonoUsuarioRepository extends JpaRepository<AbonoUsuario, Integer> {

    List<AbonoUsuario> findByIdUsuario(Integer idUsuario);

    /** Suma {@code n} abonos del tipo (crea la fila si no existe). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO abonos_usuario (id_usuario, id_item, cantidad) VALUES (:u, :i, :n) "
            + "ON CONFLICT (id_usuario, id_item) DO UPDATE SET cantidad = abonos_usuario.cantidad + :n",
            nativeQuery = true)
    int sumar(@Param("u") Integer idUsuario, @Param("i") Integer idItem, @Param("n") int n);

    /** Descuenta 1; devuelve 0 si no quedaban existencias (nunca baja de 0). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE abonos_usuario SET cantidad = cantidad - 1 "
            + "WHERE id_usuario = :u AND id_item = :i AND cantidad > 0", nativeQuery = true)
    int descontarUno(@Param("u") Integer idUsuario, @Param("i") Integer idItem);
}
