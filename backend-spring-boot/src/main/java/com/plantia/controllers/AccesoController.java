package com.plantia.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.QuickAccessDto;
import com.plantia.services.AccesoService;

@RestController
@RequestMapping("/api/v1/accesos")
public class AccesoController {

    private final AccesoService accesoService;

    public AccesoController(AccesoService accesoService) {
        this.accesoService = accesoService;
    }

    /** Alimenta QuickAccessGrid del HomeScreen. */
    @GetMapping
    public List<QuickAccessDto> listar() {
        return accesoService.listar();
    }
}
