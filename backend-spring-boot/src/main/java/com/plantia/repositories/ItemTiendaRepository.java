package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.ItemTienda;

public interface ItemTiendaRepository extends JpaRepository<ItemTienda, Integer> {

    /** El abono mas barato de la tienda. */
    Optional<ItemTienda> findFirstByTipoOrderByPrecioMonedasAsc(String tipo);
}
