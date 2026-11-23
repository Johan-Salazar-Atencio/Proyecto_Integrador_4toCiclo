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
        // NUEVO: solo mostrar especies publicadas por default
        spec = spec.and((root, cq, cb) -> cb.equal(root.get("publicada"), true));

        return catalogoRepo.findAll(spec, Sort.by(Sort.Direction.ASC, "nombreComun"))
                .stream().map(CatalogoService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CuidadoSeccionDto> cuidados(Integer idCatalogo) {
        if (idCatalogo == null) {
            return List.<CuidadoSeccionDto>of();
        }
        PlantaCatalogo catalogo = catalogoRepo.findById(idCatalogo).orElse(null);
        if (catalogo == null || !Boolean.TRUE.equals(catalogo.getPublicada())) {
            return List.<CuidadoSeccionDto>of();
        }
        return cuidadoRepo.findByIdCatalogoOrderByOrdenAsc(idCatalogo).stream()
                .map(CatalogoService::toCuidado).toList();
    }

    @Transactional(readOnly = true)
    public PlantaCatalogo obtener(Integer idCatalogo) {
        return catalogoRepo.findById(idCatalogo).orElse(null);
    }

    /**
     * Especie del catalogo para el resultado de la IA. Si ya existe NO se toca (QA-01 y QA-13): ni su nombre comun ni sus
     * cuidados, que pudo corregir el admin y que comparten todos los usuarios; antes cada escaneo los reescribia y dos
     * escaneos simultaneos de la misma especie se pisaban (500). Solo se le ponen cuidados si no tiene ninguno. Si no
     * existe se crea, con un candado por nombre para que dos escaneos a la vez no choquen con el UNIQUE (409).
     */
    @Transactional
    public PlantaCatalogo upsertDesdeIa(IaResultDto ia) {
        if (ia == null || ia.nombreCientifico() == null || ia.nombreCientifico().isBlank()) {
            return null;
        }
        String nombre = ia.nombreCientifico().trim();
        catalogoRepo.bloquearEspecie(nombre.toLowerCase(java.util.Locale.ROOT));

        java.util.Optional<PlantaCatalogo> existente = catalogoRepo.findFirstByNombreCientificoIgnoreCaseOrderByIdCatalogoAsc(nombre);
        if (existente.isPresent()) {
            PlantaCatalogo catalogo = existente.get();
            if (cuidadoRepo.countByIdCatalogo(catalogo.getIdCatalogo()) == 0) {
                guardarCuidados(catalogo, ia);
            }
            return catalogo;
        }

        PlantaCatalogo catalogo = new PlantaCatalogo();
        catalogo.setNombreCientifico(nombre);
        catalogo.setNombreComun(ia.nombreComun() != null && !ia.nombreComun().isBlank() ? ia.nombreComun() : nombre);
        catalogo.setFrecuenciaRiegoDias(7);
        catalogo.setFrecuenciaSolHoras(4);
        catalogo.setTemperaturaMinC(10);
        catalogo.setTemperaturaMaxC(30);
        catalogo.setEsToxicaMascotas(false);
        catalogo.setSpriteBaseUrl("/sprites/default.png");
        catalogo.setFechaCreacion(LocalDateTime.now());
        catalogo.setDatosIaRaw(JsonRawUtil.seguro(ia.datosRaw()));
        catalogo.setUpdatedAt(LocalDateTime.now());
        catalogo = catalogoRepo.save(catalogo);
        guardarCuidados(catalogo, ia);
        return catalogo;
    }

    private void guardarCuidados(PlantaCatalogo catalogo, IaResultDto ia) {
        if (ia.cuidados() != null && !ia.cuidados().isEmpty()) {
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
