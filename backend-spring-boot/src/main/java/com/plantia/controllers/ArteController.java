package com.plantia.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.AnimacionDto;
import com.plantia.dto.FondoDto;
import com.plantia.services.ArteService;
import com.plantia.services.TiendaService;

/** Fondos y animaciones de desastre que dibujan la web y el movil. */
@RestController
@RequestMapping("/api/v1")
public class ArteController {

    private final ArteService arteService;
    private final TiendaService tiendaService;

    public ArteController(ArteService arteService, TiendaService tiendaService) {
        this.arteService = arteService;
        this.tiendaService = tiendaService;
    }

    /** Fondos del usuario: los predeterminados y los de la tienda que tiene equipados. */
    @GetMapping("/fondos")
    public List<FondoDto> fondos(@RequestHeader("X-User-Id") Integer userId) {
        return arteService.fondos(tiendaService.fondosEquipados(userId));
    }

    @GetMapping("/animaciones")
    public List<AnimacionDto> animaciones() {
        return arteService.animaciones();
    }
}
