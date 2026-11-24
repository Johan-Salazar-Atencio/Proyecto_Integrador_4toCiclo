package com.plantia.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.AbonosDto;
import com.plantia.services.AbonoService;

/** Inventario de abonos: se compran en la tienda (suman al contador) y se usan desde la planta. */
@RestController
@RequestMapping("/api/v1/abonos")
public class AbonosController {

    private final AbonoService abonoService;

    public AbonosController(AbonoService abonoService) {
        this.abonoService = abonoService;
    }

    /** Monedas y cuantos abonos de cada tipo tiene el usuario. */
    @GetMapping
    public AbonosDto inventario(@RequestHeader("X-User-Id") Integer userId) {
        return abonoService.inventario(userId);
    }

    /**
     * Compra abonos y los suma al contador (no los usa). Body opcional: { "cantidad": 1..10 } (por defecto 1).
     * 402 sin monedas, 409 si falta nivel o llegaria a mas de 99, 404 si no es un abono de la tienda.
     */
    @PostMapping("/{idItem}/comprar")
    public AbonosDto comprar(@RequestHeader("X-User-Id") Integer userId, @PathVariable("idItem") Integer idItem,
                             @RequestBody(required = false) Map<String, Integer> body) {
        return abonoService.comprar(userId, idItem, body != null ? body.get("cantidad") : null);
    }
}
