package com.plantia.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.ColeccionDetalleDto;
import com.plantia.dto.ColeccionItemDto;
import com.plantia.services.ColeccionService;

@RestController
@RequestMapping("/api/v1/coleccion")
public class ColeccionController {

    private final ColeccionService coleccionService;

    public ColeccionController(ColeccionService coleccionService) {
        this.coleccionService = coleccionService;
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
}
