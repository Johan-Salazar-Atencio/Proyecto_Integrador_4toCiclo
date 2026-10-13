package com.plantia.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.Mision;

public interface MisionRepository extends JpaRepository<Mision, Integer> {

    List<Mision> findByActivaTrue();
}
