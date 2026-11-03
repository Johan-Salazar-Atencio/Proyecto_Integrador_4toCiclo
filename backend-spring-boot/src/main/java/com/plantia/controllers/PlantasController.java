package com.plantia.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.services.PlantasService;

@RestController
@RequestMapping("/api/v1/plantas")
public class PlantasController {

    private final PlantasService plantasService;

    public PlantasController(PlantasService plantasService) {
        this.plantasService = plantasService;
    }

    /** Crea una planta digital desde un escaneo. */
    @PostMapping
    public ResponseEntity<PlantSummaryDto> crear(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId,
            @RequestBody Map<String, Integer> body) {
        Integer idEscaneo = body.get("idEscaneo");
        if (idEscaneo == null) {
            return ResponseEntity.badRequest().build();
        }
        PlantSummaryDto creado = plantasService.crearDesdeEscaneo(userId, idEscaneo);
        return ResponseEntity.ok(creado);
    }

    /** Lista todas las plantas digitales del usuario. */
    @GetMapping
    public ResponseEntity<List<PlantSummaryDto>> listar(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        List<PlantSummaryDto> plantas = plantasService.listar(userId);
        return ResponseEntity.ok(plantas);
    }

    /** Aplica un cuidado a la planta. */
    @PostMapping("/{id}/cuidados")
    public ResponseEntity<PlantSummaryDto> cuidar(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario,
            @RequestBody Map<String, String> body) {
        String accion = body.get("accion");
        if (accion == null) {
            return ResponseEntity.badRequest().build();
        }
        PlantSummaryDto actualizado = plantasService.cuidar(idPlantaUsuario, accion);
        return ResponseEntity.ok(actualizado);
    }

    /** Activa una planta digital (cambia su etapa). */
    @PostMapping("/{id}/activar")
    public ResponseEntity<PlantSummaryDto> activar(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        PlantSummaryDto activado = plantasService.activar(idPlantaUsuario);
        return ResponseEntity.ok(activado);
    }

    /** Devuelve la planta digital activa del usuario (204 si no tiene ninguna). */
    @GetMapping("/activa")
    public ResponseEntity<PlantSummaryDto> activa(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        return plantasService.activa(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}