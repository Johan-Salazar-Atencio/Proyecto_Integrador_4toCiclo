package com.plantia.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.ColeccionDetalleDto;
import com.plantia.dto.ColeccionItemDto;
import com.plantia.models.ColeccionUsuario;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.ColeccionUsuarioRepository;
import com.plantia.repositories.PlantaCatalogoRepository;

@Service
public class ColeccionService {

    private final ColeccionUsuarioRepository coleccionRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final CatalogoService catalogoService;

    public ColeccionService(ColeccionUsuarioRepository coleccionRepo,
                            PlantaCatalogoRepository catalogoRepo,
                            CatalogoService catalogoService) {
        this.coleccionRepo = coleccionRepo;
        this.catalogoRepo = catalogoRepo;
        this.catalogoService = catalogoService;
    }

    @Transactional(readOnly = true)
    public List<ColeccionItemDto> listar(Integer userId) {
        return coleccionRepo.findByIdUsuarioOrderByUltimaVezDesc(userId).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ColeccionDetalleDto> detalle(Integer idColeccion) {
        return coleccionRepo.findById(idColeccion).map(c -> {
            var cuidados = c.getIdCatalogo() != null
                    ? catalogoService.cuidados(c.getIdCatalogo())
                    : List.<com.plantia.dto.CuidadoSeccionDto>of();
            return new ColeccionDetalleDto(map(c), cuidados);
        });
    }

    private ColeccionItemDto map(ColeccionUsuario c) {
        PlantaCatalogo cat = c.getIdCatalogo() != null
                ? catalogoRepo.findById(c.getIdCatalogo()).orElse(null)
                : null;
        return new ColeccionItemDto(
                c.getIdColeccion(),
                c.getIdCatalogo(),
                cat != null ? cat.getNombreComun() : null,
                cat != null ? cat.getNombreCientifico() : null,
                c.getFotoUrl(),
                c.getVecesVista() != null ? c.getVecesVista() : 1,
                c.getPrimeraVez(),
                c.getUltimaVez(),
                cat != null && Boolean.TRUE.equals(cat.getEsToxicaMascotas()));
    }
}
