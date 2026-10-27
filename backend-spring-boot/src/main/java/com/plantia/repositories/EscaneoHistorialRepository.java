package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.EscaneoHistorial;

public interface EscaneoHistorialRepository extends JpaRepository<EscaneoHistorial, Integer> {

    List<EscaneoHistorial> findTop20ByIdUsuarioOrderByFechaEscaneoDesc(Integer idUsuario);
}
