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
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.CatalogosPorId;
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
        List<ColeccionUsuario> filas = coleccionRepo.findByIdUsuarioOrderByUltimaVezDesc(userId);
        Map<Integer, PlantaCatalogo> catalogos = CatalogosPorId.de(catalogoRepo, filas, ColeccionUsuario::getIdCatalogo);
        return filas.stream()
                .map(c -> map(c, catalogos.get(c.getIdCatalogo())))
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

    @Transactional
    public ColeccionItemDto agregar(Integer userId, Integer idCatalogo) {
        Optional<ColeccionUsuario> existente = coleccionRepo.findByIdUsuarioAndIdCatalogo(userId, idCatalogo);
        LocalDateTime ahora = LocalDateTime.now();

        if (existente.isPresent()) {
            ColeccionUsuario c = existente.get();
            c.setVecesVista((c.getVecesVista() != null ? c.getVecesVista() : 1) + 1);
            c.setUltimaVez(ahora);
            coleccionRepo.save(c);
            return map(c);
        }

        ColeccionUsuario c = new ColeccionUsuario();
        c.setIdUsuario(userId);
        c.setIdCatalogo(idCatalogo);
        c.setIdEscaneo(null);
        c.setFotoUrl(null);
        c.setVecesVista(1);
        c.setPrimeraVez(ahora);
        c.setUltimaVez(ahora);
        coleccionRepo.save(c);
        return map(c);
    }

    private ColeccionItemDto map(ColeccionUsuario c) {
        return map(c, c.getIdCatalogo() != null ? catalogoRepo.findById(c.getIdCatalogo()).orElse(null) : null);
    }

    private ColeccionItemDto map(ColeccionUsuario c, PlantaCatalogo cat) {
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
