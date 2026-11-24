package com.plantia.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.TiendaDto;
import com.plantia.services.AbonoService;
import com.plantia.services.TiendaService;

/** Tienda del usuario: los precios y lo que se vende lo decide el admin (panel de Django). */
@RestController
@RequestMapping("/api/v1/tienda")
public class TiendaController {

    private final TiendaService tiendaService;
    private final AbonoService abonoService;

    public TiendaController(TiendaService tiendaService, AbonoService abonoService) {
        this.tiendaService = tiendaService;
        this.abonoService = abonoService;
    }

    @GetMapping
    public TiendaDto tienda(@RequestHeader("X-User-Id") Integer userId) {
        return tiendaService.listar(userId);
    }

    /**
     * Compra un articulo (402 sin monedas; 409 si ya lo tiene o falta nivel). Los fondos se ponen desde el jardin.
     * Un abono se puede comprar muchas veces: suma al contador (body opcional {"cantidad": 1..10}) y no se usa.
     */
    @PostMapping("/{id}/comprar")
    public TiendaDto comprar(@RequestHeader("X-User-Id") Integer userId, @PathVariable("id") Integer idItem,
                             @RequestBody(required = false) Map<String, Integer> body) {
        if (abonoService.esAbono(idItem)) {
            abonoService.comprar(userId, idItem, body != null ? body.get("cantidad") : null);
            return tiendaService.listar(userId);
        }
        return tiendaService.comprar(userId, idItem);
    }
}
