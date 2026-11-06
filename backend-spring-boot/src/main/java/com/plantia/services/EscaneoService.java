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
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.util.JsonRawUtil;

@Service
public class EscaneoService {

    private static final Logger log = LoggerFactory.getLogger(EscaneoService.class);

    private final EscaneoHistorialRepository escaneoRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final StorageService storage;
    private final IaClient iaClient;
    private final CatalogoService catalogoService;

    public EscaneoService(EscaneoHistorialRepository escaneoRepo,
                          PlantaCatalogoRepository catalogoRepo,
                          StorageService storage,
                          IaClient iaClient,
                          CatalogoService catalogoService) {
        this.escaneoRepo = escaneoRepo;
        this.catalogoRepo = catalogoRepo;
        this.storage = storage;
        this.iaClient = iaClient;
        this.catalogoService = catalogoService;
    }

    /**
     * Flujo completo de escaneo:
     * 1) guarda la foto  2) identifica con IA  3) upsert catalogo/cuidados
     * 4) registra escaneo  5) NO más auto-insert a coleccion (se usa POST /api/v1/coleccion)
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

        // Determinar si la planta fue identificada basándose en si hay nombre científico
        boolean identificado = ia.nombreCientifico() != null && !ia.nombreCientifico().isBlank();

        PlantaCatalogo catalogo = null;
        if (identificado) {
            catalogo = catalogoService.upsertDesdeIa(ia);
        }

        EscaneoHistorial escaneo = new EscaneoHistorial();
        escaneo.setIdUsuario(userId);
        escaneo.setIdCatalogo(catalogo != null ? catalogo.getIdCatalogo() : null);
        escaneo.setImagenFotoUrl(imagenUrl);
        escaneo.setConfianzaIa(ia.confianza() != null ? BigDecimal.valueOf(ia.confianza()) : null);
        escaneo.setRespuestaIaRaw(JsonRawUtil.seguro(ia.datosRaw() != null ? ia.datosRaw() : "{}"));
        escaneo.setFechaEscaneo(LocalDateTime.now());
        escaneo = escaneoRepo.save(escaneo);

        // Ya no insertamos automáticamente en coleccion - eso lo hace el endpoint dedicado
        // Si no está identificado, tampoco insertamos en coleccion desde aquí

        String nombreComun = catalogo != null ? catalogo.getNombreComun() : "Planta desconocida";

        return new ScanResultDto(
                escaneo.getIdEscaneo(),
                catalogo != null ? catalogo.getIdCatalogo() : null,
                nombreComun,
                catalogo != null ? catalogo.getNombreCientifico() : null,
                ia.confianza(),
                imagenUrl,
                false, // ya no es auto-insertado desde el escaneo
                catalogo != null
                        ? catalogoService.cuidados(catalogo.getIdCatalogo())
                        : List.<CuidadoSeccionDto>of(),
                identificado // nuevo campo: true solo si la IA devolvió nombre científico
        );
    }

    // Método upsertColeccion eliminado: la inserción en coleccion ahora corre por POST /api/v1/coleccion

    @Transactional(readOnly = true)
    public List<RecentScanDto> recientes(Integer userId) {
        return escaneoRepo.findTop20ByIdUsuarioOrderByFechaEscaneoDesc(userId).stream()
                .map(e -> {
                    String nombre = "Planta desconocida";
                    String color = "#F59E0B";
                    Integer confNum = 0;
                    if (e.getIdCatalogo() != null) {
                        nombre = catalogoRepo.findById(e.getIdCatalogo())
                                .map(PlantaCatalogo::getNombreComun)
                                .orElse("Planta");
                        color = "#10B981";
                        if (e.getConfianzaIa() != null) {
                            confNum = e.getConfianzaIa().intValue();
                        }
                    }
                    String confianza = confNum != null
                            ? "Confianza " + confNum + "%"
                            : "Sin identificar";
                    return new RecentScanDto(String.valueOf(e.getIdEscaneo()), nombre, confianza, confNum, color);
                })
                .toList();
    }
}