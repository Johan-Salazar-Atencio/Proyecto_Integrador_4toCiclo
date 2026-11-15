package com.plantia.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.AnimacionDto;
import com.plantia.dto.FondoDto;
import com.plantia.services.ArteService;

/** Fondos y animaciones de desastre que dibujan la web y el movil. */
@RestController
@RequestMapping("/api/v1")
public class ArteController {

    private final ArteService arteService;

    public ArteController(ArteService arteService) {
        this.arteService = arteService;
    }

    /** Escenas (dia, tarde y noche) del tema de fondo de la planta que el usuario tiene en su jardin. */
    @GetMapping("/fondos")
    public List<FondoDto> fondos(@RequestHeader("X-User-Id") Integer userId) {
        return arteService.fondos(userId);
    }

    @GetMapping("/animaciones")
    public List<AnimacionDto> animaciones() {
        return arteService.animaciones();
    }
}
