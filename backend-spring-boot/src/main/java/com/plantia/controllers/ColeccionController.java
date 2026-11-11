package com.plantia.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.ColeccionDetalleDto;
import com.plantia.dto.ColeccionItemDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.services.ColeccionService;

@RestController
@RequestMapping("/api/v1/coleccion")
public class ColeccionController {

    private final ColeccionService coleccionService;
    private final EscaneoHistorialRepository escaneoRepo;

    public ColeccionController(ColeccionService coleccionService,
                                 EscaneoHistorialRepository escaneoRepo) {
        this.coleccionService = coleccionService;
        this.escaneoRepo = escaneoRepo;
    }

    /** Listado del dashboard "Mi coleccion". */
    @GetMapping
    public List<ColeccionItemDto> listar(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        return coleccionService.listar(userId);
    }

    /** Pantalla de detalle con los cuidados de la planta. */
    @GetMapping("/{id}")
    public ResponseEntity<ColeccionDetalleDto> detalle(@PathVariable("id") Integer id) {
        return coleccionService.detalle(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Agrega una planta a la colección del usuario (idempotente).
     *  Body: { "idEscaneo": <int> }
     *  Header: X-User-Id
     *  201 si se creó nuevo, 200 si ya existía. */
    @PostMapping
    public ResponseEntity<ColeccionItemDto> agregar(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId,
            @RequestBody Map<String, Integer> body) {
        Integer idEscaneo = body.get("idEscaneo");
        if (idEscaneo == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<EscaneoHistorial> escaneoOpt = escaneoRepo.findById(idEscaneo);
        if (escaneoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EscaneoHistorial escaneo = escaneoOpt.get();
        if (escaneo.getIdCatalogo() == null) {
            return ResponseEntity.badRequest().build();
        }
        ColeccionItemDto creado = coleccionService.agregar(userId, escaneo.getIdCatalogo());
        return ResponseEntity.ok(creado);
    }
}