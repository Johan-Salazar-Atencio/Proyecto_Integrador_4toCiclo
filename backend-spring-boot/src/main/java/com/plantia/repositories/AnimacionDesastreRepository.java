package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.AnimacionDesastre;

public interface AnimacionDesastreRepository extends JpaRepository<AnimacionDesastre, Integer> {

    List<AnimacionDesastre> findByActivaTrueOrderByIdTipoDesastreAscFaseAsc();
}
