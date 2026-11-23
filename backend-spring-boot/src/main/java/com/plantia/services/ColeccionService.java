package com.plantia.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.ColeccionDetalleDto;
import com.plantia.dto.ColeccionItemDto;
import com.plantia.models.ColeccionUsuario;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.CatalogosPorId;
import com.plantia.repositories.ColeccionUsuarioRepository;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;

@Service
public class ColeccionService {

    private final ColeccionUsuarioRepository coleccionRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final CatalogoService catalogoService;
    private final EscaneoHistorialRepository escaneoRepo;

    public ColeccionService(ColeccionUsuarioRepository coleccionRepo,
                            PlantaCatalogoRepository catalogoRepo,
                            CatalogoService catalogoService,
                            EscaneoHistorialRepository escaneoRepo) {
        this.coleccionRepo = coleccionRepo;
        this.catalogoRepo = catalogoRepo;
        this.catalogoService = catalogoService;
        this.escaneoRepo = escaneoRepo;
    }

    @Transactional(readOnly = true)
    public List<ColeccionItemDto> listar(Integer userId) {
        List<ColeccionUsuario> filas = coleccionRepo.findByIdUsuarioOrderByUltimaVezDesc(userId);
        Map<Integer, PlantaCatalogo> catalogos = CatalogosPorId.de(catalogoRepo, filas, ColeccionUsuario::getIdCatalogo);
        // QA-23: las fotos que faltan salen en UNA consulta (antes una por fila: 200 filas = 207 consultas)
        List<Integer> sinFoto = filas.stream().filter(c -> c.getFotoUrl() == null && c.getIdCatalogo() != null)
                .map(ColeccionUsuario::getIdCatalogo).distinct().toList();
        Map<Integer, String> fotos = new java.util.HashMap<>();
        if (!sinFoto.isEmpty()) {
            for (Object[] fila : escaneoRepo.ultimasFotos(userId, sinFoto)) {
                fotos.put(((Number) fila[0]).intValue(), (String) fila[1]);
            }
        }
        return filas.stream()
                .map(c -> map(c, catalogos.get(c.getIdCatalogo()),
                        c.getFotoUrl() != null || c.getIdCatalogo() == null ? c.getFotoUrl() : fotos.get(c.getIdCatalogo())))
                .toList();
    }

    /** Detalle de un registro de la coleccion; vacio si no existe o es de otro usuario. */
    @Transactional(readOnly = true)
    public Optional<ColeccionDetalleDto> detalle(Integer userId, Integer idColeccion) {
        return coleccionRepo.findById(idColeccion)
                .filter(c -> userId.equals(c.getIdUsuario()))
                .map(c -> {
            var cuidados = c.getIdCatalogo() != null
                    ? catalogoService.cuidados(c.getIdCatalogo())
                    : List.<com.plantia.dto.CuidadoSeccionDto>of();
            return new ColeccionDetalleDto(map(c), cuidados);
        });
    }

    /** Guarda la planta en la coleccion con la foto del escaneo; si ya estaba, cuenta otra vista y pone la foto mas nueva. */
    @Transactional
    public ColeccionItemDto agregar(Integer userId, Integer idCatalogo, EscaneoHistorial escaneo) {
        coleccionRepo.bloquearUsuario(userId); // antes de leer: el mismo escaneo enviado 5 veces a la vez daba un 409
        Optional<ColeccionUsuario> existente = coleccionRepo.findByIdUsuarioAndIdCatalogo(userId, idCatalogo);
        LocalDateTime ahora = LocalDateTime.now();

        if (existente.isPresent()) {
            ColeccionUsuario c = existente.get();
            if (escaneo.getIdEscaneo() != null && escaneo.getIdEscaneo().equals(c.getIdEscaneo())) {
                return map(c); // QA-25: el mismo escaneo guardado dos veces no suma otra vista
            }
            c.setVecesVista((c.getVecesVista() != null ? c.getVecesVista() : 1) + 1);
            c.setUltimaVez(ahora);
            c.setIdEscaneo(escaneo.getIdEscaneo());
            c.setFotoUrl(escaneo.getImagenFotoUrl());
            coleccionRepo.save(c);
            return map(c);
        }

        ColeccionUsuario c = new ColeccionUsuario();
        c.setIdUsuario(userId);
        c.setIdCatalogo(idCatalogo);
        c.setIdEscaneo(escaneo.getIdEscaneo());
        c.setFotoUrl(escaneo.getImagenFotoUrl());
        c.setVecesVista(1);
        c.setPrimeraVez(ahora);
        c.setUltimaVez(ahora);
        coleccionRepo.save(c);
        return map(c);
    }

    /** La foto guardada; si la planta se guardo sin ella, la del escaneo mas reciente de esa especie. */
    private String fotoDe(ColeccionUsuario c) {
        if (c.getFotoUrl() != null || c.getIdCatalogo() == null) {
            return c.getFotoUrl();
        }
        return escaneoRepo.findFirstByIdUsuarioAndIdCatalogoOrderByFechaEscaneoDesc(c.getIdUsuario(), c.getIdCatalogo())
                .map(EscaneoHistorial::getImagenFotoUrl)
                .orElse(null);
    }

    private ColeccionItemDto map(ColeccionUsuario c) {
        return map(c, c.getIdCatalogo() != null ? catalogoRepo.findById(c.getIdCatalogo()).orElse(null) : null);
    }

    private ColeccionItemDto map(ColeccionUsuario c, PlantaCatalogo cat) {
        return map(c, cat, fotoDe(c));
    }

    private ColeccionItemDto map(ColeccionUsuario c, PlantaCatalogo cat, String foto) {
        return new ColeccionItemDto(
                c.getIdColeccion(),
                c.getIdCatalogo(),
                cat != null ? cat.getNombreComun() : null,
                cat != null ? cat.getNombreCientifico() : null,
                foto,
                c.getVecesVista() != null ? c.getVecesVista() : 1,
                c.getPrimeraVez(),
                c.getUltimaVez(),
                cat != null && Boolean.TRUE.equals(cat.getEsToxicaMascotas()));
    }
}
