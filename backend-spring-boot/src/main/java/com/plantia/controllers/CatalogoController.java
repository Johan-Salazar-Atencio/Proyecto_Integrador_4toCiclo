package com.plantia.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.CatalogoItemDto;
import com.plantia.dto.CuidadoSeccionDto;
import com.plantia.services.CatalogoService;

@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /** Listado / busqueda rapida. Filtros: q (texto), toxica (bool). */
    @GetMapping
    public List<CatalogoItemDto> buscar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "toxica", required = false) Boolean toxica) {
        return catalogoService.buscar(q, toxica);
    }

    /** Base de conocimiento de cuidados (secciones 1:N). */
    @GetMapping("/{id}/cuidados")
    public List<CuidadoSeccionDto> cuidados(@PathVariable("id") Integer id) {
        return catalogoService.cuidados(id);
    }
}
