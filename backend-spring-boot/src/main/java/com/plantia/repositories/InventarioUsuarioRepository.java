package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plantia.models.InventarioUsuario;

public interface InventarioUsuarioRepository extends JpaRepository<InventarioUsuario, Integer> {

    List<InventarioUsuario> findByIdUsuario(Integer idUsuario);

    List<InventarioUsuario> findByIdUsuarioAndEstaEquipadoTrue(Integer idUsuario);

    @Query("SELECT COUNT(iu) FROM InventarioUsuario iu, ItemTienda it "
            + "WHERE iu.idItem = it.idItem AND iu.idUsuario = :idUsuario "
            + "AND (it.precioMonedas IS NULL OR it.precioMonedas > 0)")
    long countComprasByUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT COUNT(iu) FROM InventarioUsuario iu, ItemTienda it "
            + "WHERE iu.idItem = it.idItem AND iu.idUsuario = :idUsuario AND it.tipo = :tipo "
            + "AND (it.precioMonedas IS NULL OR it.precioMonedas > 0)")
    long countComprasByUsuarioYTipo(@Param("idUsuario") Integer idUsuario, @Param("tipo") String tipo);
}
