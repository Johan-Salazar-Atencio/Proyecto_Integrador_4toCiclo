package com.plantia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.TipoDesastre;

public interface TipoDesastreRepository extends JpaRepository<TipoDesastre, Integer> {
}
