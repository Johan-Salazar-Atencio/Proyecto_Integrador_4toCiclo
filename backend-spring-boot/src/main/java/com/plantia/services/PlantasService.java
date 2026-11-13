package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.CatalogosPorId;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.repositories.PlantaUsuarioRepository;

@Service
public class PlantasService {

    private final PlantaUsuarioRepository plantaRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final EscaneoHistorialRepository escaneoRepo;
    private final ObjectMapper json;

    public PlantasService(PlantaUsuarioRepository plantaRepo,
                          PlantaCatalogoRepository catalogoRepo,
                          EscaneoHistorialRepository escaneoRepo,
                          ObjectMapper json) {
        this.plantaRepo = plantaRepo;
        this.catalogoRepo = catalogoRepo;
        this.escaneoRepo = escaneoRepo;
        this.json = json;
    }

    // Reglas del jardin digital (antes vivian solo en la web, en store/jardinSimulado.js)
    static final int SALUD_MARCHITA = 30;
    private static final int NECESIDAD_CRITICA = 30;
    private static final int DESGASTE_AGUA_HORA = 4;
    private static final int DESGASTE_LUZ_HORA = 3;
    private static final int DESGASTE_SALUD_HORA = 2;
    /** Cuidados acumulados para ser BROTE y ADULTA (mismos umbrales que jardin/etapas.js). */
    private static final int CRECIMIENTO_BROTE = 2;
    private static final int CRECIMIENTO_ADULTA = 9;

    /** Planta de "Mi jardin": la elegida como activa o, si no hay, la ultima plantada. */
    @Transactional
    public Optional<PlantSummaryDto> activa(Integer userId) {
        List<PlantaUsuario> plantas = plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId).stream()
                .filter(p -> !"MUERTA".equals(p.getEtapaCrecimiento()))
                .toList();
        return plantas.stream()
                .filter(p -> Boolean.TRUE.equals(p.getEsActiva()))
                .findFirst()
                .or(() -> plantas.stream().findFirst())
                .map(p -> map(plantaRepo.save(envejecer(p, LocalDateTime.now()))));
    }

    /**
     * Digitaliza una planta en el jardin del usuario y la deja como activa. Sale de un
     * escaneo propio (idEscaneo) o de una especie de su coleccion (idCatalogo). Si ya
     * tenia esa especie, se reinicia en vez de duplicarla.
     */
    @Transactional
    public PlantSummaryDto digitalizar(Integer userId, Integer idEscaneo, Integer idCatalogo,
                                       Map<String, Object> apariencia) {
        String foto = null;
        if (idEscaneo != null) {
            EscaneoHistorial escaneo = escaneoRepo.findById(idEscaneo)
                    .filter(e -> userId.equals(e.getIdUsuario()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escaneo no encontrado"));
            if (escaneo.getIdCatalogo() == null) {
                throw new IllegalArgumentException("El escaneo no tiene planta identificada");
            }
            idCatalogo = escaneo.getIdCatalogo();
            foto = escaneo.getImagenFotoUrl();
        }
        if (idCatalogo == null) {
            throw new IllegalArgumentException("Falta idEscaneo o idCatalogo");
        }
        PlantaCatalogo catalogo = catalogoRepo.findById(idCatalogo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especie no encontrada"));

        List<PlantaUsuario> suyas = plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId);
        final Integer especie = catalogo.getIdCatalogo();
        PlantaUsuario planta = suyas.stream()
                .filter(p -> especie.equals(p.getIdCatalogo()))
                .findFirst()
                .orElseGet(PlantaUsuario::new);
        suyas.forEach(p -> p.setEsActiva(false));

        LocalDateTime ahora = LocalDateTime.now();
        planta.setIdUsuario(userId);
        planta.setIdCatalogo(especie);
        planta.setApodo(catalogo.getNombreComun());
        planta.setSaludPorcentaje(90);
        planta.setHidratacionPorcentaje(60);
        planta.setSolPorcentaje(70);
        planta.setCrecimiento(0);
        planta.setEtapaCrecimiento("SEMILLA");
        planta.setModoTiempo("REAL");
        planta.setEstaRefugiada(false);
        planta.setUltimoRiego(ahora);
        planta.setUltimoSol(ahora);
        planta.setUltimoAbono(ahora);
        planta.setFechaPlantado(ahora);
        planta.setUpdatedAt(ahora);
        planta.setApariencia(aJson(apariencia));
        if (foto != null) {
            planta.setFotoUrl(foto);
        }
        planta.setEsActiva(true);

        plantaRepo.saveAll(suyas);
        return map(plantaRepo.save(planta), catalogo);
    }

    @Transactional
    public List<PlantSummaryDto> listar(Integer userId) {
        List<PlantaUsuario> plantas = plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId);
        Map<Integer, PlantaCatalogo> catalogos = CatalogosPorId.de(catalogoRepo, plantas, PlantaUsuario::getIdCatalogo);
        LocalDateTime ahora = LocalDateTime.now();
        plantas.forEach(p -> envejecer(p, ahora));
        plantaRepo.saveAll(plantas);
        return plantas.stream()
                .map(p -> map(p, catalogos.get(p.getIdCatalogo())))
                .toList();
    }

    /** Elige la planta que se muestra en "Mi jardin". */
    @Transactional
    public PlantSummaryDto seleccionar(Integer userId, Integer idPlantaUsuario) {
        PlantaUsuario elegida = propia(userId, idPlantaUsuario);
        List<PlantaUsuario> suyas = plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId);
        suyas.forEach(p -> p.setEsActiva(p.getIdPlantaUsuario().equals(elegida.getIdPlantaUsuario())));
        plantaRepo.saveAll(suyas);
        elegida.setEsActiva(true);
        return map(elegida);
    }

    /**
     * Aplica un cuidado. Si sube algo (no estaba al 100 %), cuenta como cuidado util y la
     * planta crece, salvo que este marchita: entonces primero hay que recuperarla.
     */
    @Transactional
    public PlantSummaryDto cuidar(Integer userId, Integer idPlantaUsuario, String accion) {
        LocalDateTime ahora = LocalDateTime.now();
        PlantaUsuario p = envejecer(propia(userId, idPlantaUsuario), ahora);
        boolean util;
        switch (accion) {
            case "REGAR" -> {
                util = valor(p.getHidratacionPorcentaje()) < 100;
                p.setHidratacionPorcentaje(tope(valor(p.getHidratacionPorcentaje()) + 20));
                p.setUltimoRiego(ahora);
            }
            case "SOL" -> {
                util = valor(p.getSolPorcentaje()) < 100;
                p.setSolPorcentaje(tope(valor(p.getSolPorcentaje()) + 15));
                p.setUltimoSol(ahora);
            }
            case "ABONAR" -> {
                util = valor(p.getSaludPorcentaje()) < 100;
                p.setSaludPorcentaje(tope(valor(p.getSaludPorcentaje()) + 10));
                p.setUltimoAbono(ahora);
            }
            default -> throw new IllegalArgumentException("Acción inválida: " + accion);
        }
        if (util && valor(p.getSaludPorcentaje()) >= SALUD_MARCHITA) {
            p.setCrecimiento(valor(p.getCrecimiento()) + 1);
            p.setEtapaCrecimiento(etapaSegun(p.getCrecimiento()));
        }
        plantaRepo.save(p);
        return map(p);
    }

    /** Activa una planta digital (avanza su etapa: SEMILLA -> BROTE -> ADULTA). */
    @Transactional
    public PlantSummaryDto activar(Integer userId, Integer idPlantaUsuario) {
        PlantaUsuario p = propia(userId, idPlantaUsuario);
        String etapa = p.getEtapaCrecimiento() == null ? "SEMILLA" : p.getEtapaCrecimiento();
        String siguiente = switch (etapa) {
            case "SEMILLA" -> "BROTE";
            case "BROTE" -> "ADULTA";
            default -> "ADULTA";
        };
        p.setEtapaCrecimiento(siguiente);
        p.setUpdatedAt(LocalDateTime.now());
        plantaRepo.save(p);
        return map(p);
    }

    /**
     * Desgaste por horas completas sin cuidados: el agua y la luz bajan solas y, si alguna
     * queda bajo el 30 %, la salud tambien. Solo se descuentan horas enteras y updated_at
     * avanza esas mismas horas, asi las consultas frecuentes no "pierden" desgaste
     * (en la simulacion de la web, recargar a menudo evitaba que la planta se secara).
     */
    static PlantaUsuario envejecer(PlantaUsuario p, LocalDateTime ahora) {
        LocalDateTime desde = p.getUpdatedAt() != null ? p.getUpdatedAt() : ahora;
        long horas = Duration.between(desde, ahora).toHours();
        if (horas <= 0) {
            if (p.getUpdatedAt() == null) {
                p.setUpdatedAt(ahora);
            }
            return p;
        }
        int agua = tope(valor(p.getHidratacionPorcentaje()) - (int) Math.min(100, DESGASTE_AGUA_HORA * horas));
        int luz = tope(valor(p.getSolPorcentaje()) - (int) Math.min(100, DESGASTE_LUZ_HORA * horas));
        p.setHidratacionPorcentaje(agua);
        p.setSolPorcentaje(luz);
        if (agua < NECESIDAD_CRITICA || luz < NECESIDAD_CRITICA) {
            p.setSaludPorcentaje(tope(valor(p.getSaludPorcentaje()) - (int) Math.min(100, DESGASTE_SALUD_HORA * horas)));
        }
        p.setUpdatedAt(desde.plusHours(horas));
        return p;
    }

    static String etapaSegun(int crecimiento) {
        if (crecimiento >= CRECIMIENTO_ADULTA) {
            return "ADULTA";
        }
        return crecimiento >= CRECIMIENTO_BROTE ? "BROTE" : "SEMILLA";
    }

    private static int tope(int n) {
        return Math.max(0, Math.min(100, n));
    }

    private static int valor(Integer n) {
        return n != null ? n : 0;
    }

    private String aJson(Map<String, Object> apariencia) {
        if (apariencia == null || apariencia.isEmpty()) {
            return null;
        }
        try {
            return json.writeValueAsString(apariencia);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> deJson(String apariencia) {
        if (apariencia == null || apariencia.isBlank()) {
            return null;
        }
        try {
            return json.readValue(apariencia, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** La planta si es del usuario; 404 si no existe o es de otro (no se revela que existe). */
    private PlantaUsuario propia(Integer userId, Integer idPlantaUsuario) {
        return plantaRepo.findById(idPlantaUsuario)
                .filter(p -> userId.equals(p.getIdUsuario()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planta no encontrada"));
    }

    private PlantSummaryDto map(PlantaUsuario p) {
        return map(p, p.getIdCatalogo() != null ? catalogoRepo.findById(p.getIdCatalogo()).orElse(null) : null);
    }

    private PlantSummaryDto map(PlantaUsuario p, PlantaCatalogo cat) {
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
                etapaUrl(cat, p.getEtapaCrecimiento()),
                p.getIdPlantaUsuario(),
                valor(p.getCrecimiento()),
                deJson(p.getApariencia()),
                p.getFotoUrl(),
                Boolean.TRUE.equals(p.getEsActiva()));
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