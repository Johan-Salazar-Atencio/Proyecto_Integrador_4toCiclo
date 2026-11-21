package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.Logro;

public interface LogroRepository extends JpaRepository<Logro, Integer> {

    List<Logro> findByActivoTrueOrderByOrdenAscIdLogroAsc();

    List<Logro> findAllByOrderByOrdenAscIdLogroAsc();

    Optional<Logro> findByCodigo(String codigo);
}
