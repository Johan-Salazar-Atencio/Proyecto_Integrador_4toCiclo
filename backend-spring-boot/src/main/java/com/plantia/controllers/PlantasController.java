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

    /**
     * Digitaliza una planta en "Mi jardin" y la deja como activa.
     * Body: { "idEscaneo": n } o { "idCatalogo": n }, y opcional "apariencia": {...} (de la IA).
     */
    @PostMapping
    public ResponseEntity<PlantSummaryDto> crear(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer idEscaneo = entero(body.get("idEscaneo"));
        Integer idCatalogo = entero(body.get("idCatalogo"));
        if (idEscaneo == null && idCatalogo == null) {
            return ResponseEntity.badRequest().build();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> apariencia = body.get("apariencia") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        return ResponseEntity.ok(plantasService.digitalizar(userId, idEscaneo, idCatalogo, apariencia));
    }

    /** Elige la planta que se muestra en "Mi jardin". */
    @PostMapping("/{id}/seleccionar")
    public PlantSummaryDto seleccionar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return plantasService.seleccionar(userId, idPlantaUsuario);
    }

    private static Integer entero(Object valor) {
        return valor instanceof Number n ? n.intValue() : null;
    }

    /** Lista todas las plantas digitales del usuario. */
    @GetMapping
    public ResponseEntity<List<PlantSummaryDto>> listar(
            @RequestHeader("X-User-Id") Integer userId) {
        List<PlantSummaryDto> plantas = plantasService.listar(userId);
        return ResponseEntity.ok(plantas);
    }

    /** Aplica un cuidado a la planta. */
    @PostMapping("/{id}/cuidados")
    public ResponseEntity<PlantSummaryDto> cuidar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario,
            @RequestBody Map<String, String> body) {
        String accion = body.get("accion");
        if (accion == null) {
            return ResponseEntity.badRequest().build();
        }
        PlantSummaryDto actualizado = plantasService.cuidar(userId, idPlantaUsuario, accion);
        return ResponseEntity.ok(actualizado);
    }

    /** Activa una planta digital (cambia su etapa). */
    @PostMapping("/{id}/activar")
    public ResponseEntity<PlantSummaryDto> activar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        PlantSummaryDto activado = plantasService.activar(userId, idPlantaUsuario);
        return ResponseEntity.ok(activado);
    }

    /** Devuelve la planta digital activa del usuario (204 si no tiene ninguna). */
    @GetMapping("/activa")
    public ResponseEntity<PlantSummaryDto> activa(
            @RequestHeader("X-User-Id") Integer userId) {
        return plantasService.activa(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}