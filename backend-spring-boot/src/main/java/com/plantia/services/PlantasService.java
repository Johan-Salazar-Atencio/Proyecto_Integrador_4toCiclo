package com.plantia.services;

import java.time.Duration;
import java.time.LocalDate;
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
import com.plantia.dto.ProgresoDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaCatalogo;
import com.plantia.models.Usuario;
import com.plantia.repositories.CatalogosPorId;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.UsuarioRepository;

@Service
public class PlantasService {

    private final PlantaUsuarioRepository plantaRepo;
    private final PlantaCatalogoRepository catalogoRepo;
    private final EscaneoHistorialRepository escaneoRepo;
    private final ProgresoService progresoService;
    private final EventoService eventoService;
    private final UsuarioRepository usuarioRepo;
    private final ParametrosJuego parametros;
    private final ItemTiendaRepository itemRepo;
    private final ObjectMapper json;

    public PlantasService(PlantaUsuarioRepository plantaRepo,
                          PlantaCatalogoRepository catalogoRepo,
                          EscaneoHistorialRepository escaneoRepo,
                          ProgresoService progresoService,
                          EventoService eventoService,
                          UsuarioRepository usuarioRepo,
                          ParametrosJuego parametros,
                          ItemTiendaRepository itemRepo,
                          ObjectMapper json) {
        this.plantaRepo = plantaRepo;
        this.catalogoRepo = catalogoRepo;
        this.escaneoRepo = escaneoRepo;
        this.progresoService = progresoService;
        this.eventoService = eventoService;
        this.usuarioRepo = usuarioRepo;
        this.parametros = parametros;
        this.itemRepo = itemRepo;
        this.json = json;
    }

    // Reglas del jardin digital (antes vivian solo en la web, en store/jardinSimulado.js)
    static final int SALUD_MARCHITA = 30;
    private static final int NECESIDAD_CRITICA = 30;
    private static final int DESGASTE_AGUA_HORA = 4;
    private static final int DESGASTE_LUZ_HORA = 3;
    private static final int DESGASTE_SALUD_HORA = 2;

    /**
     * Planta de "Mi jardin": la elegida como activa o, si no hay, la ultima plantada viva. Una planta
     * muerta tambien puede ser la activa, para que el usuario vea el boton de revivirla.
     */
    @Transactional
    public Optional<PlantSummaryDto> activa(Integer userId) {
        // Las plantas del baul no se muestran en el jardin
        List<PlantaUsuario> plantas = plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId).stream()
                .filter(p -> !enBaul(p))
                .toList();
        return plantas.stream()
                .filter(p -> Boolean.TRUE.equals(p.getEsActiva()))
                .findFirst()
                .or(() -> plantas.stream().filter(p -> !"MUERTA".equals(p.getEtapaCrecimiento())).findFirst())
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
        // Reiniciar una especie que ya tenia no da XP: si no, se podria repetir para ganar
        boolean nueva = planta.getIdPlantaUsuario() == null;
        if (!nueva && enBaul(planta)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa planta esta en el baul: sacala primero");
        }
        // Un jardin lleno no recibe plantas nuevas: hay que mandar una al baul
        int tope = parametros.maxPlantasActivas();
        if (nueva && activasDe(suyas) >= tope) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tu jardin esta lleno (" + tope + " plantas). Manda una al baul para sumar otra.");
        }
        suyas.forEach(p -> p.setEsActiva(false));

        LocalDateTime ahora = LocalDateTime.now();
        planta.setIdUsuario(userId);
        planta.setIdCatalogo(especie);
        planta.setApodo(catalogo.getNombreComun());
        planta.setSaludPorcentaje(90);
        planta.setHidratacionPorcentaje(60);
        planta.setSolPorcentaje(70);
        planta.setCrecimiento(0);
        planta.setDiasCuidados(0);
        planta.setUltimoDiaCuidado(null);
        planta.setUltimoDiaAbonoTienda(null);
        planta.setEtapaCrecimiento("SEMILLA");
        planta.setModoTiempo("REAL");
        planta.setEstaRefugiada(false);
        planta.setRecuperacionHasta(null);
        planta.setGolpeTipo(null);
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
        PlantaUsuario guardada = plantaRepo.save(planta);
        PlantSummaryDto dto = map(guardada, catalogo);
        return nueva
                ? dto.conProgreso(progresoService.otorgar(userId, List.of("DIGITALIZAR"), List.of("DIGITALIZAR", "COLECCION"), guardada.getIdPlantaUsuario()))
                : dto;
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
        exigirFueraDelBaul(elegida);
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
        exigirFueraDelBaul(p);
        if ("MUERTA".equals(p.getEtapaCrecimiento())) {
            plantaRepo.save(p);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planta esta muerta: hay que revivirla");
        }
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
                // Una planta golpeada por un evento solo se recupera con abono, y cada uno rinde mucho mas
                int sube = p.getRecuperacionHasta() != null ? parametros.abonoRecuperacion() : 10;
                p.setSaludPorcentaje(tope(valor(p.getSaludPorcentaje()) + sube));
                if (valor(p.getSaludPorcentaje()) >= 100) {
                    p.setRecuperacionHasta(null);
                }
                p.setUltimoAbono(ahora);
            }
            default -> throw new IllegalArgumentException("Acción inválida: " + accion);
        }
        if (util && valor(p.getSaludPorcentaje()) >= SALUD_MARCHITA) {
            p.setCrecimiento(valor(p.getCrecimiento()) + 1);
            contarDia(p, ahora.toLocalDate(), CrecimientoPlanta.diasTotales(deJson(p.getApariencia())));
        }
        plantaRepo.save(p);
        PlantSummaryDto dto = map(p);
        // Solo un cuidado util da hojas y XP (regar una planta al 100 % no cuenta); cada regla tiene su tope diario
        return util ? dto.conProgreso(progresoService.otorgar(userId, List.of(motivoDeCuidado(accion)), accionesDeCuidado(accion), p.getIdPlantaUsuario())) : dto;
    }

    /** Regla de progreso que paga cada cuidado: son pocas hojas y XP, para que no se abuse. */
    static String motivoDeCuidado(String accion) {
        return switch (accion) {
            case "REGAR" -> "REGAR";
            case "SOL" -> "SOL";
            default -> "ABONAR";
        };
    }

    /** Tipos de mision que avanza un cuidado: el suyo y CUIDADO, que cuenta cualquiera de los tres. */
    static List<String> accionesDeCuidado(String accion) {
        return List.of(misionDeCuidado(accion), "CUIDADO");
    }

    /**
     * Previene el evento en curso en una planta (antes "refugiar"). Solo se puede con un evento activo
     * para la region del usuario y con la planta viva; repetirlo es inofensivo. El resultado (SALVADA o
     * golpeada) y el premio llegan cuando el evento termina (ResultadoDesastreService), por eso aqui no se da XP.
     */
    @Transactional
    public PlantSummaryDto prevenir(Integer userId, Integer idPlantaUsuario) {
        PlantaUsuario p = propia(userId, idPlantaUsuario);
        exigirFueraDelBaul(p);
        if ("MUERTA".equals(p.getEtapaCrecimiento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planta ya no se puede proteger");
        }
        if (!Boolean.TRUE.equals(p.getEstaRefugiada())) {
            if (eventoService.activo(userId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay un evento activo");
            }
            // updated_at no se toca: envejecer() lo usa para contar las horas de desgaste
            p.setEstaRefugiada(true);
            LocalDateTime ahora = LocalDateTime.now();
            p.setRefugiadaDesde(ahora);
            plantaRepo.save(p);
            // Cuenta para las misiones de tipo REFUGIO; solo se informa si la mision pago algo
            ProgresoDto premio = progresoService.registrarAccion(userId, "REFUGIO", p.getIdPlantaUsuario(), ahora);
            if (premio.xpGanado() != 0 || premio.monedasGanadas() != 0) {
                return map(p).conProgreso(premio);
            }
        }
        return map(p);
    }

    /** Nombre anterior de {@link #prevenir}; se conserva para las apps que aun llaman a /refugio. */
    @Transactional
    public PlantSummaryDto refugiar(Integer userId, Integer idPlantaUsuario) {
        return prevenir(userId, idPlantaUsuario);
    }

    /** Tipo de mision que cuenta cada cuidado. */
    private static String misionDeCuidado(String accion) {
        return switch (accion) {
            case "REGAR" -> "RIEGO";
            case "SOL" -> "SOL";
            default -> "ABONO";
        };
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
        // Una planta muerta o guardada en el baul no cambia con el tiempo
        if ("MUERTA".equals(p.getEtapaCrecimiento()) || enBaul(p)) {
            return p;
        }
        // Una planta golpeada por un evento que no se abono a tiempo muere
        if (p.getRecuperacionHasta() != null && ahora.isAfter(p.getRecuperacionHasta())) {
            morir(p);
            return p;
        }
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

    /** El usuario ya vio la animacion del evento que golpeo a la planta: no se vuelve a mostrar. */
    @Transactional
    public PlantSummaryDto golpeVisto(Integer userId, Integer idPlantaUsuario) {
        PlantaUsuario p = propia(userId, idPlantaUsuario);
        if (p.getGolpeTipo() != null) {
            p.setGolpeTipo(null);
            plantaRepo.save(p);
        }
        return map(p);
    }

    /** Plantas del jardin (las que no estan en el baul); cuentan para el tope, incluso las muertas, que se pueden revivir. */
    static int activasDe(List<PlantaUsuario> plantas) {
        return (int) plantas.stream().filter(p -> !enBaul(p)).count();
    }

    static boolean enBaul(PlantaUsuario p) {
        return Boolean.TRUE.equals(p.getEnBaul());
    }

    private static void exigirFueraDelBaul(PlantaUsuario p) {
        if (enBaul(p)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planta esta en el baul: sacala primero");
        }
    }

    /**
     * Guarda la planta en el baul: queda congelada tal cual esta (no crece, no se desgasta, no le afectan los eventos
     * ni da hojas o XP) y libera un lugar del jardin. No se puede con una planta golpeada por un evento: primero hay que
     * recuperarla, porque si no el baul serviria para esquivar su plazo.
     */
    @Transactional
    public PlantSummaryDto guardarEnBaul(Integer userId, Integer idPlantaUsuario) {
        PlantaUsuario p = envejecer(propia(userId, idPlantaUsuario), LocalDateTime.now());
        if (!enBaul(p)) {
            if (p.getRecuperacionHasta() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Primero recupera la planta: esta golpeada por un evento");
            }
            p.setEnBaul(true);
            p.setEsActiva(false);
            p.setEstaRefugiada(false);
            p.setRefugiadaDesde(null);
            plantaRepo.save(p);
        }
        return map(p);
    }

    /** Saca la planta del baul y la devuelve al jardin; 409 si el jardin ya esta lleno. El tiempo guardado no cuenta como desgaste. */
    @Transactional
    public PlantSummaryDto sacarDelBaul(Integer userId, Integer idPlantaUsuario) {
        PlantaUsuario p = propia(userId, idPlantaUsuario);
        if (enBaul(p)) {
            int tope = parametros.maxPlantasActivas();
            if (activasDe(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId)) >= tope) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tu jardin esta lleno (" + tope + " plantas). Manda otra al baul para sacar esta.");
            }
            p.setEnBaul(false);
            p.setUpdatedAt(LocalDateTime.now());
            plantaRepo.save(p);
        }
        return map(p);
    }

    /** Cambia el apodo de la planta (maximo 50 caracteres); vacio vuelve al nombre comun de la especie. */
    @Transactional
    public PlantSummaryDto renombrar(Integer userId, Integer idPlantaUsuario, String apodo) {
        PlantaUsuario p = propia(userId, idPlantaUsuario);
        String limpio = apodo == null ? "" : apodo.trim();
        if (limpio.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El apodo no puede pasar de 50 caracteres");
        }
        if (limpio.isEmpty()) {
            limpio = p.getIdCatalogo() == null ? "Planta"
                    : catalogoRepo.findById(p.getIdCatalogo()).map(PlantaCatalogo::getNombreComun).orElse("Planta");
        }
        p.setApodo(limpio);
        plantaRepo.save(p);
        return map(p);
    }

    /** La planta muere: salud en cero, sin plazo de recuperacion. Conserva sus dias para poder revivirla. */
    static void morir(PlantaUsuario p) {
        p.setEtapaCrecimiento("MUERTA");
        p.setSaludPorcentaje(0);
        p.setRecuperacionHasta(null);
        p.setEstaRefugiada(false);
        p.setRefugiadaDesde(null);
    }

    /**
     * Revive una planta muerta a cambio de hojas (el costo lo fija el admin). Vuelve con media salud y
     * con la fase que le correspondia por sus dias de cuidado. 409 si no esta muerta, 402 sin hojas.
     */
    @Transactional
    public PlantSummaryDto revivir(Integer userId, Integer idPlantaUsuario) {
        usuarioRepo.bloquear(userId); // antes de leer el saldo: evita revivir dos veces pagando una
        PlantaUsuario p = propia(userId, idPlantaUsuario);
        exigirFueraDelBaul(p);
        if (!"MUERTA".equals(p.getEtapaCrecimiento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planta no esta muerta");
        }
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        int costo = parametros.costoRevivir();
        if (valor(u.getMonedas()) < costo) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "No te alcanzan las hojas");
        }
        u.setMonedas(valor(u.getMonedas()) - costo);
        usuarioRepo.save(u);

        LocalDateTime ahora = LocalDateTime.now();
        p.setSaludPorcentaje(parametros.saludRevivida());
        p.setHidratacionPorcentaje(50);
        p.setSolPorcentaje(50);
        p.setRecuperacionHasta(null);
        p.setGolpeTipo(null);
        p.setEstaRefugiada(false);
        p.setRefugiadaDesde(null);
        p.setEtapaCrecimiento(CrecimientoPlanta.faseSegun(valor(p.getDiasCuidados()),
                CrecimientoPlanta.diasTotales(deJson(p.getApariencia()))));
        p.setUltimoRiego(ahora);
        p.setUltimoSol(ahora);
        p.setUpdatedAt(ahora);
        return map(plantaRepo.save(p));
    }

    /**
     * Cuenta el dia de hoy para el crecimiento (una sola vez por dia, hasta los dias totales)
     * y sube la fase si corresponde. La planta no retrocede de fase.
     */
    static void contarDia(PlantaUsuario p, LocalDate hoy, int diasTotales) {
        if (!hoy.equals(p.getUltimoDiaCuidado())) {
            sumarDia(p, diasTotales);
            p.setUltimoDiaCuidado(hoy);
        } else {
            subirFase(p, diasTotales);
        }
    }

    /** Suma un dia de crecimiento (sin pasar del total) y sube la fase si corresponde. */
    static void sumarDia(PlantaUsuario p, int diasTotales) {
        p.setDiasCuidados(Math.min(diasTotales, valor(p.getDiasCuidados()) + 1));
        subirFase(p, diasTotales);
    }

    /**
     * Suma medios dias de crecimiento (un abono suave da 1, uno normal 2, uno reforzado 6). Si queda un medio dia
     * suelto, se guarda y el siguiente abono lo completa; al llegar al total no sobra nada.
     */
    static void sumarMediosDias(PlantaUsuario p, int medios, int diasTotales) {
        int total = medios + (p.isMedioDiaPendiente() ? 1 : 0);
        int nuevos = Math.min(diasTotales, valor(p.getDiasCuidados()) + total / 2);
        p.setDiasCuidados(nuevos);
        p.setMedioDiaPendiente(total % 2 == 1 && nuevos < diasTotales);
        subirFase(p, diasTotales);
    }

    private static void subirFase(PlantaUsuario p, int diasTotales) {
        String fase = CrecimientoPlanta.faseSegun(valor(p.getDiasCuidados()), diasTotales);
        if (CrecimientoPlanta.rango(fase) > CrecimientoPlanta.rango(p.getEtapaCrecimiento())) {
            p.setEtapaCrecimiento(fase);
        }
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

    /** Resumen de una planta propia (404 si no es del usuario). */
    @Transactional
    public PlantSummaryDto resumen(Integer userId, Integer idPlantaUsuario) {
        return map(propia(userId, idPlantaUsuario));
    }

    Map<String, Object> apariencia(PlantaUsuario p) {
        return deJson(p.getApariencia());
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
                Boolean.TRUE.equals(p.getEsActiva()),
                Boolean.TRUE.equals(p.getEstaRefugiada()),
                valor(p.getDiasCuidados()),
                CrecimientoPlanta.diasTotales(deJson(p.getApariencia())),
                p.getEtapaCrecimiento(),
                p.getRecuperacionHasta() != null,
                segundosHasta(p.getRecuperacionHasta()),
                p.getGolpeTipo(),
                "MUERTA".equals(p.getEtapaCrecimiento()) && parametros != null ? parametros.costoRevivir() : null,
                enBaul(p),
                macetaDe(p),
                null);
    }

    /** Codigo de la maceta puesta; la terracota si no eligio otra (o si el articulo ya no existe). */
    private String macetaDe(PlantaUsuario p) {
        if (p.getIdMaceta() == null || itemRepo == null) {
            return "TERRACOTA";
        }
        return itemRepo.findById(p.getIdMaceta()).map(ItemTienda::getCodigo).orElse("TERRACOTA");
    }

    /** Segundos que faltan para el momento dado (0 si ya paso); null si no hay momento. */
    private static Long segundosHasta(LocalDateTime momento) {
        return momento == null ? null : Math.max(0, Duration.between(LocalDateTime.now(), momento).getSeconds());
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