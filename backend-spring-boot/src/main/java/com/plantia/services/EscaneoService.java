package com.plantia.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.plantia.dto.CuidadoSeccionDto;
import com.plantia.dto.IaResultDto;
import com.plantia.dto.ProgresoDto;
import com.plantia.dto.RecentScanDto;
import com.plantia.dto.ScanResultDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaCatalogo;
import com.plantia.repositories.CatalogosPorId;
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
    private final ProgresoService progresoService;
    private final TransactionTemplate transaccion;

    public EscaneoService(EscaneoHistorialRepository escaneoRepo,
                          PlantaCatalogoRepository catalogoRepo,
                          StorageService storage,
                          IaClient iaClient,
                          CatalogoService catalogoService,
                          ProgresoService progresoService,
                          PlatformTransactionManager transactionManager) {
        this.escaneoRepo = escaneoRepo;
        this.catalogoRepo = catalogoRepo;
        this.storage = storage;
        this.iaClient = iaClient;
        this.catalogoService = catalogoService;
        this.progresoService = progresoService;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /**
     * Flujo completo de escaneo:
     * 1) guarda la foto  2) identifica con IA  3) upsert catalogo/cuidados
     * 4) registra escaneo  5) NO más auto-insert a coleccion (se usa POST /api/v1/coleccion)
     *
     * Sin @Transactional a proposito: la llamada a la IA tarda hasta 60 s y, dentro de
     * una transaccion, retendria una conexion del pool todo ese tiempo (con ~10 escaneos
     * a la vez se agotaba el pool y se colgaba toda la API). Solo los pasos 3-4 van en
     * una transaccion corta; si fallan, se borra la foto ya guardada.
     */
    public ScanResultDto escanear(Integer userId, MultipartFile foto) {
        // QA-39: primero se valida y guarda la foto (un archivo que no es imagen da 415 y ya no gasta el tope)
        String imagenUrl = storage.save(foto);
        Long marca;
        try {
            marca = limitarPorUsuario(userId, System.currentTimeMillis());
        } catch (RuntimeException e) {
            storage.delete(imagenUrl);
            throw e;
        }
        try {
            IaResultDto ia = identificar(foto); // fuera de la transaccion
            return transaccion.execute(estado -> registrar(userId, imagenUrl, ia));
        } catch (RuntimeException e) {
            storage.delete(imagenUrl);
            devolverCupo(userId, marca); // un escaneo que fallo (IA saturada, error) no cuenta para el tope
            throw e;
        }
    }

    /** Escaneos por usuario y minuto (QA-08); 0 o menos lo desactiva. */
    @org.springframework.beans.factory.annotation.Value("${app.escaneo.max-por-minuto:10}")
    int maxPorMinuto = 10;
    private final java.util.concurrent.ConcurrentHashMap<Integer, java.util.ArrayDeque<Long>> recientes = new java.util.concurrent.ConcurrentHashMap<>();

    /** Reserva un cupo del minuto y devuelve su marca (null si no hay tope); 429 si ya no quedan. */
    Long limitarPorUsuario(Integer userId, long ahoraMs) {
        if (maxPorMinuto <= 0 || userId == null) {
            return null;
        }
        java.util.ArrayDeque<Long> marcas = recientes.computeIfAbsent(userId, k -> new java.util.ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahoraMs - marcas.peekFirst() >= 60_000) {
                marcas.pollFirst();
            }
            if (marcas.size() >= maxPorMinuto) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        "Escaneaste muchas plantas seguidas. Espera un minuto y vuelve a intentarlo.");
            }
            marcas.addLast(ahoraMs);
            return ahoraMs;
        }
    }

    void devolverCupo(Integer userId, Long marca) {
        java.util.ArrayDeque<Long> marcas = marca == null ? null : recientes.get(userId);
        if (marcas != null) {
            synchronized (marcas) {
                marcas.removeLastOccurrence(marca);
            }
        }
    }

    private IaResultDto identificar(MultipartFile foto) {
        IaResultDto ia;
        try {
            ia = iaClient.identificar(foto);
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            // QA-08: el motor de IA esta saturado. Antes se guardaba como "Planta desconocida", sin premio ni aviso.
            log.warn("Motor IA saturado (429)");
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Hay muchas identificaciones en este momento. Intenta de nuevo en un minuto.");
        } catch (Exception e) {
            log.warn("Motor IA no disponible: {}", e.getMessage());
            ia = IaClient.fallback();
        }
        return ia;
    }

    private ScanResultDto registrar(Integer userId, String imagenUrl, IaResultDto ia) {
        boolean identificado = esIdentificado(ia);
        if (identificado) {
            // El usuario se bloquea ANTES de guardar el escaneo: el INSERT toma un candado compartido sobre su
            // fila (FK a usuarios) y, si el FOR UPDATE de ProgresoService llegara despues, dos escaneos
            // simultaneos del mismo usuario se esperarian entre si (deadlock).
            progresoService.bloquearUsuario(userId);
        }

        PlantaCatalogo catalogo = null;
        if (identificado) {
            catalogo = catalogoService.upsertDesdeIa(ia);
        }
        if (catalogo != null && Boolean.FALSE.equals(catalogo.getPublicada())) {
            // QA-42: el admin retiro la especie del catalogo: el escaneo se guarda como no identificado (sin premio y
            // sin ofrecer digitalizar). Antes decia "identificada", pagaba y digitalizar daba 404.
            catalogo = null;
            identificado = false;
        }

        // Se mira antes de guardar el escaneo: despues, la especie ya contaria como vista
        boolean especieNueva = catalogo != null
                && !escaneoRepo.existsByIdUsuarioAndIdCatalogo(userId, catalogo.getIdCatalogo());

        EscaneoHistorial escaneo = new EscaneoHistorial();
        escaneo.setIdUsuario(userId);
        escaneo.setIdCatalogo(catalogo != null ? catalogo.getIdCatalogo() : null);
        escaneo.setImagenFotoUrl(imagenUrl);
        escaneo.setConfianzaIa(ia.confianza() != null ? BigDecimal.valueOf(ia.confianza()) : null);
        escaneo.setRespuestaIaRaw(JsonRawUtil.seguro(ia.datosRaw() != null ? ia.datosRaw() : "{}"));
        escaneo.setFechaEscaneo(LocalDateTime.now());
        // QA-12: la apariencia de la IA queda en el servidor; digitalizar la toma de aqui
        escaneo.setApariencia(identificado ? com.plantia.util.AparienciaUtil.aJson(ia.apariencia()) : null);
        escaneo = escaneoRepo.save(escaneo);

        // Ya no insertamos automáticamente en coleccion - eso lo hace el endpoint dedicado
        // Si no está identificado, tampoco insertamos en coleccion desde aquí

        // Solo un escaneo identificado da hojas y XP: una especie nueva paga mucho mas que repetir una ya vista
        ProgresoDto progreso = null;
        if (identificado) {
            progreso = progresoService.otorgar(userId, motivosDeEscaneo(especieNueva), List.of("ESCANEO"), escaneo.getIdEscaneo());
        }

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
                identificado, // nuevo campo: true solo si la IA devolvió nombre científico
                identificado ? com.plantia.util.AparienciaUtil.limpiar(ia.apariencia()) : null,
                progreso
        );
    }

    /** Confianza minima (0-100) para dar por buena una especie: por debajo la IA esta adivinando. */
    static final double CONFIANZA_MINIMA = 50.0;

    /**
     * Identificado solo si la IA devolvio una especie concreta con confianza suficiente. Si no, el
     * escaneo se guarda como "sin identificar": no crea entrada en el catalogo ni paga hojas/XP.
     * Antes bastaba confianza > 0 y las respuestas genericas ("Plantae sp.", "Plantae indet.", 10 %)
     * creaban especies falsas que pagaban ESCANEO_NUEVA cada vez.
     */
    static boolean esIdentificado(IaResultDto ia) {
        if (ia.confianza() == null || ia.confianza() < CONFIANZA_MINIMA) {
            return false;
        }
        String nombre = ia.nombreCientifico() == null ? "" : ia.nombreCientifico().trim().toLowerCase();
        if (nombre.isBlank() || !nombre.matches(".*[a-z].*")) {
            return false;
        }
        // Reino o marcadores de "no se": no es una especie
        return !nombre.startsWith("plantae")
                && !nombre.startsWith("planta ")
                && !nombre.matches(".*\\b(desconocid[ao]|unknown|indet\\.?|indeterminad[ao]|gen[eé]ric[ao]|no identificad[ao]|no identificable)\\b.*");
    }

    /**
     * Regla que paga un escaneo identificado. ESCANEO_NUEVA (especie que el usuario nunca habia escaneado)
     * da mucho mas que ESCANEO_REPETIDO, que ademas tiene un tope diario: repetir la misma planta no rinde.
     */
    static List<String> motivosDeEscaneo(boolean especieNueva) {
        return List.of(especieNueva ? "ESCANEO_NUEVA" : "ESCANEO_REPETIDO");
    }

    // Método upsertColeccion eliminado: la inserción en coleccion ahora corre por POST /api/v1/coleccion

    @Transactional(readOnly = true)
    public List<RecentScanDto> recientes(Integer userId) {
        List<EscaneoHistorial> escaneos = escaneoRepo.findTop20ByIdUsuarioOrderByFechaEscaneoDesc(userId);
        Map<Integer, PlantaCatalogo> catalogos = CatalogosPorId.de(catalogoRepo, escaneos, EscaneoHistorial::getIdCatalogo);
        return escaneos.stream()
                .map(e -> {
                    String nombre = "Planta desconocida";
                    String color = "#F59E0B";
                    Integer confNum = 0;
                    if (e.getIdCatalogo() != null) {
                        PlantaCatalogo cat = catalogos.get(e.getIdCatalogo());
                        nombre = cat != null ? cat.getNombreComun() : "Planta";
                        color = "#10B981";
                        if (e.getConfianzaIa() != null) {
                            confNum = e.getConfianzaIa().intValue();
                        }
                    }
                    // confNum nunca es null: el texto depende de si hubo especie (antes siempre "Confianza 0%")
                    String confianza = e.getIdCatalogo() != null
                            ? "Confianza " + confNum + "%"
                            : "Sin identificar";
                    return new RecentScanDto(String.valueOf(e.getIdEscaneo()), nombre, confianza, confNum, color, e.getImagenFotoUrl());
                })
                .toList();
    }
}