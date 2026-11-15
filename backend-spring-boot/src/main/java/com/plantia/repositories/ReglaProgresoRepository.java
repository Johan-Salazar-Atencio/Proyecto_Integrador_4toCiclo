package com.plantia.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.ReglaProgreso;

public interface ReglaProgresoRepository extends JpaRepository<ReglaProgreso, Integer> {

    Optional<ReglaProgreso> findByMotivo(String motivo);
}
