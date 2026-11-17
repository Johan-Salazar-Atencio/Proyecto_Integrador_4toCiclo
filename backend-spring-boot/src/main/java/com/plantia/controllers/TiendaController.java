package com.plantia.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.TiendaDto;
import com.plantia.services.TiendaService;

/** Tienda del usuario: los precios y lo que se vende lo decide el admin (panel de Django). */
@RestController
@RequestMapping("/api/v1/tienda")
public class TiendaController {

    private final TiendaService tiendaService;

    public TiendaController(TiendaService tiendaService) {
        this.tiendaService = tiendaService;
    }

    @GetMapping
    public TiendaDto tienda(@RequestHeader("X-User-Id") Integer userId) {
        return tiendaService.listar(userId);
    }

    /** Compra un tema de fondo (402 sin monedas; 409 si ya lo tiene o falta nivel). Se pone desde el jardin. */
    @PostMapping("/{id}/comprar")
    public TiendaDto comprar(@RequestHeader("X-User-Id") Integer userId, @PathVariable("id") Integer idItem) {
        return tiendaService.comprar(userId, idItem);
    }
}
