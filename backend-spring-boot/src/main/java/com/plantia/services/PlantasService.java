package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.PlantaCatalogo;
import com.plantia.models.PlantaUsuario;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.repositories.PlantaUsuarioRepository;

@Service
public class PlantasService {

    private final PlantaUsuarioRepository plantaRepo;
    private final PlantaCatalogoRepository catalogoRepo;

    public PlantasService(PlantaUsuarioRepository plantaRepo, PlantaCatalogoRepository catalogoRepo) {
        this.plantaRepo = plantaRepo;
        this.catalogoRepo = catalogoRepo;
    }

    @Transactional(readOnly = true)
    public Optional<PlantSummaryDto> activa(Integer userId) {
        return plantaRepo
                .findFirstByIdUsuarioAndEtapaCrecimientoNotOrderByFechaPlantadoDesc(userId, "MUERTA")
                .map(this::map);
    }

    private PlantSummaryDto map(PlantaUsuario p) {
        PlantaCatalogo cat = p.getIdCatalogo() != null
                ? catalogoRepo.findById(p.getIdCatalogo()).orElse(null)
                : null;

        String nombre = p.getApodo() != null && !p.getApodo().isBlank()
                ? p.getApodo()
                : (cat != null ? cat.getNombreComun() : "Planta");
        String especie = cat != null ? cat.getNombreCientifico() : "";

        return new PlantSummaryDto(
                nombre,
                especie,
                p.getSaludPorcentaje() != null ? p.getSaludPorcentaje() : 0,
                p.getHidratacionPorcentaje() != null ? p.getHidratacionPorcentaje() : 0,
                p.getSolPorcentaje() != null ? p.getSolPorcentaje() : 0,
                desde(p.getUltimoRiego()),
                etapaUrl(cat, p.getEtapaCrecimiento()));
    }

    private static String etapaUrl(PlantaCatalogo cat, String etapa) {
        if (cat == null || etapa == null) {
            return cat != null ? cat.getSpriteBaseUrl() : null;
        }
        return switch (etapa) {
            case "SEMILLA" -> cat.getEtapaSemillaUrl() != null ? cat.getEtapaSemillaUrl() : cat.getSpriteBaseUrl();
            case "BROTE" -> cat.getEtapaBroteUrl() != null ? cat.getEtapaBroteUrl() : cat.getSpriteBaseUrl();
            case "ADULTA" -> cat.getEtapaAdultaUrl() != null ? cat.getEtapaAdultaUrl() : cat.getSpriteBaseUrl();
            default -> cat.getSpriteBaseUrl();
        };
    }

    private static String desde(LocalDateTime momento) {
        if (momento == null) {
            return "Sin registro";
        }
        Duration d = Duration.between(momento, LocalDateTime.now());
        long horas = d.toHours();
        if (horas < 1) {
            return "Hace unos minutos";
        }
        if (horas < 24) {
            return "Hace " + horas + " horas";
        }
        return "Hace " + d.toDays() + " dias";
    }
}
