package com.plantia.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.Region;

public interface RegionRepository extends JpaRepository<Region, Integer> {

    List<Region> findByActivaTrueOrderByNombreAsc();

    Optional<Region> findByCodigoAndActivaTrue(String codigo);
}
