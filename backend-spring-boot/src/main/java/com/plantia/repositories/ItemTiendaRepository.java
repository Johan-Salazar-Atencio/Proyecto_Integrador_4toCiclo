package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.ItemTienda;

public interface ItemTiendaRepository extends JpaRepository<ItemTienda, Integer> {

    /** El articulo activo mas barato de ese tipo (se usa para el abono). */
    Optional<ItemTienda> findFirstByTipoAndActivoTrueOrderByPrecioMonedasAsc(String tipo);

    /** Todos los articulos de un tipo (activos o no), del mas barato al mas caro. */
    List<ItemTienda> findByTipoOrderByPrecioMonedasAscIdItemAsc(String tipo);

    List<ItemTienda> findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc();
}
