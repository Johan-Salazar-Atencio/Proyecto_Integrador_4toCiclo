package com.plantia.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.repositories.PlantaUsuarioRepository;

@Service
public class PlantasService {

    private final PlantaUsuarioRepository plantaRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final EscaneoHistorialRepository escaneoRepo;

    public PlantasService(PlantaUsuarioRepository plantaRepo,
                          PlantaCatalogoRepository catalogoRepo,
                          EscaneoHistorialRepository escaneoRepo) {
        this.plantaRepo = plantaRepo;
        this.catalogoRepo = catalogoRepo;
        this.escaneoRepo = escaneoRepo;
    }

    @Transactional(readOnly = true)
    public Optional<PlantSummaryDto> activa(Integer userId) {
        return plantaRepo
                .findFirstByIdUsuarioAndEtapaCrecimientoNotOrderByFechaPlantadoDesc(userId, "MUERTA")
                .map(this::map);
    }

    @Transactional
    public PlantSummaryDto crearDesdeEscaneo(Integer userId, Integer idEscaneo) {
        Optional<EscaneoHistorial> escaneoOpt = escaneoRepo.findById(idEscaneo);
        if (escaneoOpt.isEmpty()) {
            throw new IllegalArgumentException("Escaneo no encontrado");
        }
        EscaneoHistorial escaneo = escaneoOpt.get();
        if (escaneo.getIdCatalogo() == null) {
            throw new IllegalArgumentException("El escaneo no tiene planta identificada");
        }

        PlantaCatalogo catalogo = escaneo.getIdCatalogo();

        PlantaUsuario planta = new PlantaUsuario();
        planta.setIdUsuario(userId);
        planta.setIdCatalogo(catalogo.getIdCatalogo());
        planta.setApodo(catalogo.getNombreComun());
        planta.setSaludPorcentaje(90);
        planta.setHidratacionPorcentaje(60);
        planta.setSolPorcentaje(70);
        planta.setEtapaCrecimiento("SEMILLA");
        planta.setModoTiempo("REAL");
        planta.setEstaRefugiada(false);
        planta.setUltimoRiego(LocalDateTime.now());
        planta.setUltimoSol(LocalDateTime.now());
        planta.setUltimoAbono(LocalDateTime.now());
        planta.setFechaPlantado(LocalDateTime.now());
        planta.setUpdatedAt(LocalDateTime.now());

        planta = plantaRepo.save(planta);
        return map(planta);
    }

    @Transactional(readOnly = true)
    public List<PlantSummaryDto> listar(Integer userId) {
        return plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public PlantSummaryDto cuidar(Integer idPlantaUsuario, String accion) {
        Optional<PlantaUsuario> opt = plantaRepo.findById(idPlantaUsuario);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Planta no encontrada");
        }
        PlantaUsuario p = opt.get();
        LocalDateTime ahora = LocalDateTime.now();

        switch (accion) {
            case "REGAR":
                int nuevaHidratacion = Math.min(100, p.getHidratacionPorcentaje() + 20);
                p.setHidratacionPorcentaje(nuevaHidratacion);
                p.setUltimoRiego(ahora);
                break;
            case "SOL":
                int nuevaLuz = Math.min(100, p.getSolPorcentaje() + 15);
                p.setSolPorcentaje(nuevaLuz);
                break;
            case "ABONAR":
                int nuevaSalud = Math.min(100, p.getSaludPorcentaje() + 10);
                p.setSaludPorcentaje(nuevaSalud);
                break;
            default:
                throw new IllegalArgumentException("Acción inválida: " + accion);
        }
        p.setUpdatedAt(LocalDateTime.now());
        plantaRepo.save(p);
        return map(p);
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