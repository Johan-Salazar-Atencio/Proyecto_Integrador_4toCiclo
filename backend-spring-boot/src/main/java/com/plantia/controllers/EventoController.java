package com.plantia.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.WeatherAlertDto;
import com.plantia.services.EventoService;

@RestController
@RequestMapping("/api/v1/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Alimenta WeatherAlertCard. 204 si no hay desastre activo para la region. */
    @GetMapping("/activo")
    public ResponseEntity<WeatherAlertDto> activo(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        return eventoService.activo(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
