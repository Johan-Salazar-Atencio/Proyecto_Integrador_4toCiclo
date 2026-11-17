package com.plantia.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.DailyMissionDto;
import com.plantia.services.MisionEventoService;
import com.plantia.services.MisionService;

@RestController
@RequestMapping("/api/v1/misiones")
public class MisionController {

    private final MisionService misionService;
    private final MisionEventoService misionEventoService;

    public MisionController(MisionService misionService, MisionEventoService misionEventoService) {
        this.misionService = misionService;
        this.misionEventoService = misionEventoService;
    }

    /** Alimenta DailyMissionCard. 204 si no hay misiones activas. */
    @GetMapping("/diaria")
    public ResponseEntity<DailyMissionDto> diaria(
            @RequestHeader("X-User-Id") Integer userId) {
        return misionService.diaria(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Todas las misiones activas de hoy con el avance del usuario (las define el admin). Si hay un evento
     * activo, su mision (kind = "EVENT") va primera.
     */
    @GetMapping
    public List<DailyMissionDto> deHoy(@RequestHeader("X-User-Id") Integer userId) {
        List<DailyMissionDto> todas = new ArrayList<>();
        misionEventoService.deEvento(userId).ifPresent(todas::add);
        todas.addAll(misionService.deHoy(userId));
        return todas;
    }
}
