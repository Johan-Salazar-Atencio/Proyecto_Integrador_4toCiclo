package com.plantia.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.RegionDto;
import com.plantia.services.UsuarioService;

@RestController
@RequestMapping("/api/v1/regiones")
public class RegionController {

    private final UsuarioService usuarioService;

    public RegionController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Regiones activas, para el selector del perfil. */
    @GetMapping
    public List<RegionDto> listar() {
        return usuarioService.regiones();
    }
}
