package com.plantia.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.services.PlantasService;

@RestController
@RequestMapping("/api/v1/plantas")
public class PlantasController {

    private final PlantasService plantasService;

    public PlantasController(PlantasService plantasService) {
        this.plantasService = plantasService;
    }

    /** Alimenta PlantSummaryCard. 204 si el usuario no tiene plantas en crianza. */
    @GetMapping("/activa")
    public ResponseEntity<PlantSummaryDto> activa(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        return plantasService.activa(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
