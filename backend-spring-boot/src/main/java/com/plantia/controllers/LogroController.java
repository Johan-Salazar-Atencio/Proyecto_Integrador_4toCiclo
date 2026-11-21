package com.plantia.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.LogroDesbloqueadoDto;
import com.plantia.dto.LogrosResponseDto;
import com.plantia.dto.LogrosVistosRequest;
import com.plantia.services.LogroService;

@RestController
@RequestMapping("/api/v1/logros")
public class LogroController {

    private final LogroService logroService;

    public LogroController(LogroService logroService) {
        this.logroService = logroService;
    }

    @GetMapping
    public LogrosResponseDto listar(@RequestHeader("X-User-Id") Integer userId) {
        return logroService.listar(userId, LocalDateTime.now());
    }

    @GetMapping("/pendientes")
    public List<LogroDesbloqueadoDto> pendientes(@RequestHeader("X-User-Id") Integer userId) {
        return logroService.pendientes(userId, LocalDateTime.now());
    }

    @PostMapping("/vistos")
    public ResponseEntity<Void> vistos(@RequestHeader("X-User-Id") Integer userId,
                                       @RequestBody(required = false) LogrosVistosRequest body) {
        logroService.marcarVistos(userId, body == null ? List.of() : body.ids());
        return ResponseEntity.noContent().build();
    }
}
