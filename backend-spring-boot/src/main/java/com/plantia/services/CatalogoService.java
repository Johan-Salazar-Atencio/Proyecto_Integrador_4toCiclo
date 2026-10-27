package com.plantia.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.CatalogoItemDto;
import com.plantia.dto.CuidadoSeccionDto;
import com.plantia.dto.IaResultDto;
import com.plantia.models.CuidadoSeccion;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.CuidadoSeccionRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.util.JsonRawUtil;

@Service
public class CatalogoService {

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "RIEGO", "LUZ", "SUSTRATO", "TEMPERATURA", "PODA", "PLAGAS", "TOXICIDAD", "ABONO", "OTRO");

    private final PlantaCatalogoRepository catalogoRepo;
    private final CuidadoSeccionRepository cuidadoRepo;

    public CatalogoService(PlantaCatalogoRepository catalogoRepo,
                           CuidadoSeccionRepository cuidadoRepo) {
        this.catalogoRepo = catalogoRepo;
        this.cuidadoRepo = cuidadoRepo;
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemDto> buscar(String q, Boolean toxica) {
        String query = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();

        Specification<PlantaCatalogo> spec = (root, cq, cb) -> cb.conjunction();
        if (query != null) {
            String like = "%" + query + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nombreComun")), like),
                    cb.like(cb.lower(root.get("nombreCientifico")), like)));
        }
        if (toxica != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("esToxicaMascotas"), toxica));
        }

        return catalogoRepo.findAll(spec, Sort.by(Sort.Direction.ASC, "nombreComun"))
                .stream().map(CatalogoService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CuidadoSeccionDto> cuidados(Integer idCatalogo) {
        return cuidadoRepo.findByIdCatalogoOrderByOrdenAsc(idCatalogo).stream()
                .map(CatalogoService::toCuidado).toList();
    }

    @Transactional(readOnly = true)
    public PlantaCatalogo obtener(Integer idCatalogo) {
        return catalogoRepo.findById(idCatalogo).orElse(null);
    }

    /**
     * Crea o actualiza la planta del catalogo a partir del resultado de la IA
     * y reemplaza sus secciones de cuidados.
     */
    @Transactional
    public PlantaCatalogo upsertDesdeIa(IaResultDto ia) {
        if (ia == null || ia.nombreCientifico() == null || ia.nombreCientifico().isBlank()) {
            return null;
        }

        PlantaCatalogo catalogo = catalogoRepo.findByNombreCientificoIgnoreCase(ia.nombreCientifico())
                .orElseGet(() -> {
                    PlantaCatalogo nueva = new PlantaCatalogo();
                    nueva.setNombreCientifico(ia.nombreCientifico());
                    nueva.setNombreComun(ia.nombreComun() != null ? ia.nombreComun() : ia.nombreCientifico());
                    nueva.setFrecuenciaRiegoDias(7);
                    nueva.setFrecuenciaSolHoras(4);
                    nueva.setTemperaturaMinC(10);
                    nueva.setTemperaturaMaxC(30);
                    nueva.setEsToxicaMascotas(false);
                    nueva.setSpriteBaseUrl("/sprites/default.png");
                    nueva.setFechaCreacion(LocalDateTime.now());
                    return nueva;
                });

        if (ia.nombreComun() != null && !ia.nombreComun().isBlank()) {
            catalogo.setNombreComun(ia.nombreComun());
        }
        catalogo.setDatosIaRaw(JsonRawUtil.seguro(ia.datosRaw()));
        catalogo.setUpdatedAt(LocalDateTime.now());
        catalogo = catalogoRepo.save(catalogo);

        if (ia.cuidados() != null && !ia.cuidados().isEmpty()) {
            cuidadoRepo.deleteByIdCatalogo(catalogo.getIdCatalogo());
            int orden = 0;
            for (IaResultDto.IaCuidadoDto cu : ia.cuidados()) {
                CuidadoSeccion seccion = new CuidadoSeccion();
                seccion.setIdCatalogo(catalogo.getIdCatalogo());
                seccion.setTipo(normalizarTipo(cu.tipo()));
                seccion.setTitulo(cu.titulo() != null ? cu.titulo() : cu.tipo());
                seccion.setContenido(cu.contenido() != null ? cu.contenido() : "");
                seccion.setOrden(orden++);
                seccion.setGeneradoPorIa(true);
                seccion.setFechaCreacion(LocalDateTime.now());
                cuidadoRepo.save(seccion);
            }
        }
        return catalogo;
    }

    private static String normalizarTipo(String tipo) {
        if (tipo == null) {
            return "OTRO";
        }
        String t = tipo.trim().toUpperCase();
        return TIPOS_VALIDOS.contains(t) ? t : "OTRO";
    }

    public static CatalogoItemDto toDto(PlantaCatalogo p) {
        return new CatalogoItemDto(
                p.getIdCatalogo(),
                p.getNombreComun(),
                p.getNombreCientifico(),
                p.getFrecuenciaRiegoDias() != null ? p.getFrecuenciaRiegoDias() : 0,
                p.getFrecuenciaSolHoras() != null ? p.getFrecuenciaSolHoras() : 0,
                p.getTemperaturaMinC(),
                p.getTemperaturaMaxC(),
                Boolean.TRUE.equals(p.getEsToxicaMascotas()),
                p.getSpriteBaseUrl());
    }

    public static CuidadoSeccionDto toCuidado(CuidadoSeccion c) {
        return new CuidadoSeccionDto(
                c.getIdSeccion(),
                c.getTipo(),
                c.getTitulo(),
                c.getContenido(),
                c.getOrden() != null ? c.getOrden() : 0,
                Boolean.TRUE.equals(c.getGeneradoPorIa()));
    }
}
