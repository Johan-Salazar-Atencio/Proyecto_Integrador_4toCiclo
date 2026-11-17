package com.plantia.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plantia.models.FrameAnimacion;

public interface FrameAnimacionRepository extends JpaRepository<FrameAnimacion, Integer> {

    List<FrameAnimacion> findByIdAnimacionInOrderByIdAnimacionAscOrdenAsc(Collection<Integer> idsAnimacion);
}
