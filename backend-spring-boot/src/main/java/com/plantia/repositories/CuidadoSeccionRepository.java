package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.CuidadoSeccion;

public interface CuidadoSeccionRepository extends JpaRepository<CuidadoSeccion, Integer> {

    List<CuidadoSeccion> findByIdCatalogoOrderByOrdenAsc(Integer idCatalogo);

    void deleteByIdCatalogo(Integer idCatalogo);
}
