package com.plantia.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.plantia.dto.IaResultDto;
import com.plantia.dto.RecentScanDto;
import com.plantia.dto.ScanResultDto;
import com.plantia.models.ColeccionUsuario;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.ColeccionUsuarioRepository;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;

@Service
public class EscaneoService {

    private static final Logger log = LoggerFactory.getLogger(EscaneoService.class);

    private final EscaneoHistorialRepository escaneoRepo;
    private final ColeccionUsuarioRepository coleccionRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final StorageService storage;
    private final IaClient iaClient;
    private final CatalogoService catalogoService;

    public EscaneoService(EscaneoHistorialRepository escaneoRepo,
                          ColeccionUsuarioRepository coleccionRepo,
                          PlantaCatalogoRepository catalogoRepo,
                          StorageService storage,
                          IaClient iaClient,
                          CatalogoService catalogoService) {
        this.escaneoRepo = escaneoRepo;
        this.coleccionRepo = coleccionRepo;
        this.catalogoRepo = catalogoRepo;
        this.storage = storage;
        this.iaClient = iaClient;
        this.catalogoService = catalogoService;
    }

    /**
     * Flujo completo de escaneo:
     * 1) guarda la foto  2) identifica con IA  3) upsert catalogo/cuidados
     * 4) registra escaneo  5) upsert coleccion.
     */
    @Transactional
    public ScanResultDto escanear(Integer userId, MultipartFile foto) {
        String imagenUrl = storage.save(foto);

        IaResultDto ia;
        try {
            ia = iaClient.identificar(foto);
        } catch (Exception e) {
            log.warn("Motor IA no disponible: {}", e.getMessage());
            ia = IaClient.fallback();
        }

        PlantaCatalogo catalogo = catalogoService.upsertDesdeIa(ia);

        EscaneoHistorial escaneo = new EscaneoHistorial();
        escaneo.setIdUsuario(userId);
        escaneo.setIdCatalogo(catalogo != null ? catalogo.getIdCatalogo() : null);
        escaneo.setImagenFotoUrl(imagenUrl);
        escaneo.setConfianzaIa(ia.confianza() != null ? BigDecimal.valueOf(ia.confianza()) : null);
        escaneo.setRespuestaIaRaw(ia.datosRaw() != null ? ia.datosRaw() : "{}");
        escaneo.setFechaEscaneo(LocalDateTime.now());
        escaneo = escaneoRepo.save(escaneo);

        boolean nuevoEnColeccion = false;
        if (catalogo != null) {
            nuevoEnColeccion = upsertColeccion(userId, catalogo.getIdCatalogo(), escaneo.getIdEscaneo(), imagenUrl);
        }

        return new ScanResultDto(
                escaneo.getIdEscaneo(),
                catalogo != null ? catalogo.getIdCatalogo() : null,
                catalogo != null ? catalogo.getNombreComun() : "Planta desconocida",
                catalogo != null ? catalogo.getNombreCientifico() : null,
                ia.confianza(),
                imagenUrl,
                nuevoEnColeccion,
                catalogo != null
                        ? catalogoService.cuidados(catalogo.getIdCatalogo())
                        : List.<com.plantia.dto.CuidadoSeccionDto>of());
    }

    private boolean upsertColeccion(Integer userId, Integer idCatalogo, Integer idEscaneo, String fotoUrl) {
        Optional<ColeccionUsuario> existente = coleccionRepo.findByIdUsuarioAndIdCatalogo(userId, idCatalogo);
        LocalDateTime ahora = LocalDateTime.now();

        if (existente.isPresent()) {
            ColeccionUsuario c = existente.get();
            c.setVecesVista((c.getVecesVista() != null ? c.getVecesVista() : 1) + 1);
            c.setUltimaVez(ahora);
            c.setFotoUrl(fotoUrl);
            c.setIdEscaneo(idEscaneo);
            coleccionRepo.save(c);
            return false;
        }

        ColeccionUsuario c = new ColeccionUsuario();
        c.setIdUsuario(userId);
        c.setIdCatalogo(idCatalogo);
        c.setIdEscaneo(idEscaneo);
        c.setFotoUrl(fotoUrl);
        c.setVecesVista(1);
        c.setPrimeraVez(ahora);
        c.setUltimaVez(ahora);
        coleccionRepo.save(c);
        return true;
    }

    @Transactional(readOnly = true)
    public List<RecentScanDto> recientes(Integer userId) {
        return escaneoRepo.findTop20ByIdUsuarioOrderByFechaEscaneoDesc(userId).stream()
                .map(e -> {
                    String nombre = "Planta desconocida";
                    String color = "#F59E0B";
                    if (e.getIdCatalogo() != null) {
                        nombre = catalogoRepo.findById(e.getIdCatalogo())
                                .map(PlantaCatalogo::getNombreComun)
                                .orElse("Planta");
                        color = "#10B981";
                    }
                    String confianza = e.getConfianzaIa() != null
                            ? "Confianza " + e.getConfianzaIa().stripTrailingZeros().toPlainString() + "%"
                            : "Sin identificar";
                    return new RecentScanDto(String.valueOf(e.getIdEscaneo()), nombre, confianza, color);
                })
                .toList();
    }
}
