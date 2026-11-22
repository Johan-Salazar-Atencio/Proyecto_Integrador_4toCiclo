package com.plantia.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.LogroDesbloqueadoDto;
import com.plantia.dto.LogroDto;
import com.plantia.dto.LogrosResponseDto;
import com.plantia.models.Logro;
import com.plantia.models.LogroUsuario;
import com.plantia.models.Usuario;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.LogroRepository;
import com.plantia.repositories.LogroUsuarioRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.RegistroDesastreUsuarioRepository;
import com.plantia.repositories.UsuarioRepository;

/**
 * Evalua y paga los logros. Regla central: un logro se desbloquea y se paga UNA sola vez por usuario.
 *
 * <ul>
 *   <li>{@link #evaluar} bloquea al usuario (SELECT ... FOR UPDATE) antes de leer nada: dos evaluaciones
 *       simultaneas (GET /logros, el scheduler de desastres, una compra) van una detras de otra.</li>
 *   <li>Las acciones del juego llaman a {@link #evaluarSeguro}: si hay una transaccion abierta, la
 *       evaluacion espera a que haga commit y corre en una transaccion propia. Asi un fallo de logros
 *       nunca tumba el escaneo, el riego, la compra ni el cierre de un evento, y tampoco deja un
 *       premio a medias (o se desbloquea y paga todo, o nada).</li>
 *   <li>En una peticion web la evaluacion NO abre una segunda conexion mientras la accion conserva la suya
 *       (eso agotaba el pool con 10 acciones simultaneas): tras el commit solo se anota en la peticion y se
 *       evalua antes de escribir la respuesta ({@code LogrosTrasLaAccion}), cuando la conexion ya se solto.</li>
 * </ul>
 */
@Service
public class LogroService {

    private static final Logger log = LoggerFactory.getLogger(LogroService.class);
    static final String MOTIVO_LOGRO = "LOGRO";
    static final int MAX_VUELTAS = 3;
    static final String ZONA_POR_DEFECTO = "America/Lima";
    /** Clave del recurso de transaccion con las evaluaciones que esperan el commit. */
    private static final Object PENDIENTES_TRAS_COMMIT = new Object();
    /** Atributo de la peticion web con las evaluaciones que esperan a que se suelte la conexion. */
    static final String PENDIENTES_PETICION = LogroService.class.getName() + ".pendientes";
    /** Ids que acepta POST /logros/vistos de una vez. */
    static final int MAX_VISTOS = 100;
    /** Rareza que se muestra de un secreto bloqueado: COMUN es el valor neutro que web y movil ya pintan. */
    static final String RAREZA_SECRETA = "COMUN";

    static final Set<String> CONDICIONES = Set.of(
            "ESCANEOS_TOTAL", "ESPECIES_DISTINTAS", "DESASTRE_AFRONTADO", "DESASTRES_TIPOS_DISTINTOS",
            "PLANTAS_REFUGIADAS", "EVENTOS_SIN_PERDIDAS", "RIEGOS_TOTAL", "ABONOS_TOTAL", "LUZ_TOTAL",
            "RACHA_DIAS", "NIVEL", "COMPRAS_TIENDA", "MACETAS_COMPRADAS", "INVERNADEROS_COMPRADOS",
            "FONDOS_COMPRADOS", "MISIONES_COMPLETADAS", "ESCANEO_HORA");

    private final LogroRepository logroRepo;
    private final LogroUsuarioRepository logroUsuarioRepo;
    private final UsuarioRepository usuarioRepo;
    private final EscaneoHistorialRepository escaneoRepo;
    private final RegistroDesastreUsuarioRepository registroDesastreRepo;
    private final MisionUsuarioRepository misionUsuarioRepo;
    private final InventarioUsuarioRepository inventarioRepo;
    private final ObjectProvider<ProgresoService> progresoService;
    /** Transaccion nueva (REQUIRES_NEW) para evaluar aparte; null en pruebas sin base de datos. */
    private final TransactionTemplate transaccionPropia;
    private final ZoneId zonaJugador;
    private final ZoneId zonaServidor;

    @Autowired
    public LogroService(LogroRepository logroRepo,
                        LogroUsuarioRepository logroUsuarioRepo,
                        UsuarioRepository usuarioRepo,
                        EscaneoHistorialRepository escaneoRepo,
                        RegistroDesastreUsuarioRepository registroDesastreRepo,
                        MisionUsuarioRepository misionUsuarioRepo,
                        InventarioUsuarioRepository inventarioRepo,
                        ObjectProvider<ProgresoService> progresoService,
                        PlatformTransactionManager transactionManager,
                        @Value("${app.logros.zona-horaria:" + ZONA_POR_DEFECTO + "}") String zonaHoraria) {
        this.logroRepo = logroRepo;
        this.logroUsuarioRepo = logroUsuarioRepo;
        this.usuarioRepo = usuarioRepo;
        this.escaneoRepo = escaneoRepo;
        this.registroDesastreRepo = registroDesastreRepo;
        this.misionUsuarioRepo = misionUsuarioRepo;
        this.inventarioRepo = inventarioRepo;
        this.progresoService = progresoService;
        if (transactionManager != null) {
            this.transaccionPropia = new TransactionTemplate(transactionManager);
            this.transaccionPropia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        } else {
            this.transaccionPropia = null;
        }
        this.zonaJugador = ZoneId.of(zonaHoraria == null || zonaHoraria.isBlank() ? ZONA_POR_DEFECTO : zonaHoraria.trim());
        // fecha_escaneo se escribe con LocalDateTime.now(): esta en la hora local del servidor (JVM)
        this.zonaServidor = ZoneId.systemDefault();
    }

    /** Compatibilidad para pruebas sin base de datos: evalua en linea y usa America/Lima. */
    public LogroService(LogroRepository logroRepo,
                        LogroUsuarioRepository logroUsuarioRepo,
                        UsuarioRepository usuarioRepo,
                        EscaneoHistorialRepository escaneoRepo,
                        RegistroDesastreUsuarioRepository registroDesastreRepo,
                        MisionUsuarioRepository misionUsuarioRepo,
                        InventarioUsuarioRepository inventarioRepo,
                        ObjectProvider<ProgresoService> progresoService) {
        this(logroRepo, logroUsuarioRepo, usuarioRepo, escaneoRepo, registroDesastreRepo, misionUsuarioRepo,
                inventarioRepo, progresoService, null, ZONA_POR_DEFECTO);
    }

    // ------------------------------------------------------------------ evaluacion segura

    /**
     * Punto de entrada para las acciones del juego. Nunca lanza.
     * <p>
     * Con una transaccion abierta (escaneo, riego, compra, cierre de evento) no evalua ahora: devuelve una
     * lista que se llena despues, en una transaccion propia y solo si la accion hizo commit. Varias llamadas
     * para el mismo usuario comparten una sola evaluacion.
     * <ul>
     *   <li>En una peticion web, el commit solo la anota en la peticion y se evalua antes de escribir la
     *       respuesta, cuando la conexion de la accion ya volvio al pool ({@link #evaluarPendientesDeLaPeticion}).
     *       {@code ProgresoDto.logrosNuevos} sigue llegando en la misma respuesta.</li>
     *   <li>Sin peticion (scheduler de eventos, un solo hilo) se evalua en el afterCommit.</li>
     * </ul>
     * <p>
     * Sin transaccion abierta (GET /logros), evalua ya, tambien en transaccion propia.
     */
    public List<LogroDesbloqueadoDto> evaluarSeguro(Integer userId, LocalDateTime ahora) {
        if (userId == null) {
            return List.of();
        }
        if (transaccionPropia != null
                && TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            return pendienteTrasCommit(userId, ahora);
        }
        return evaluarEnTransaccionPropia(userId, ahora);
    }

    private List<LogroDesbloqueadoDto> evaluarEnTransaccionPropia(Integer userId, LocalDateTime ahora) {
        try {
            List<LogroDesbloqueadoDto> nuevos = transaccionPropia == null
                    ? evaluar(userId, ahora, MAX_VUELTAS)
                    : transaccionPropia.execute(estado -> evaluar(userId, ahora, MAX_VUELTAS));
            return nuevos == null ? List.of() : nuevos;
        } catch (RuntimeException e) {
            // Su transaccion ya se deshizo entera: ni desbloqueo ni premio a medias. Se reintenta en la siguiente.
            log.error("No se pudieron evaluar logros para el usuario {}", userId, e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<LogroDesbloqueadoDto> pendienteTrasCommit(Integer userId, LocalDateTime ahora) {
        Map<Integer, Pendiente> pendientes =
                (Map<Integer, Pendiente>) TransactionSynchronizationManager.getResource(PENDIENTES_TRAS_COMMIT);
        if (pendientes == null) {
            Map<Integer, Pendiente> nuevos = new LinkedHashMap<>();
            pendientes = nuevos;
            TransactionSynchronizationManager.bindResource(PENDIENTES_TRAS_COMMIT, nuevos);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Aqui la conexion de la accion sigue tomada: en una peticion web no se pide otra
                    RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
                    if (peticion != null) {
                        encolarEnPeticion(peticion, nuevos);
                        return;
                    }
                    for (Map.Entry<Integer, Pendiente> p : nuevos.entrySet()) {
                        p.getValue().lista().addAll(evaluarEnTransaccionPropia(p.getKey(), p.getValue().ahora()));
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(PENDIENTES_TRAS_COMMIT);
                }
            });
        }
        return pendientes.computeIfAbsent(userId,
                id -> new Pendiente(ahora, Collections.synchronizedList(new ArrayList<>()))).lista();
    }

    private record Pendiente(LocalDateTime ahora, List<LogroDesbloqueadoDto> lista) {}

    /** Usuario a evaluar al final de la peticion y las listas (de varias transacciones) que esperan el resultado. */
    private record PendientePeticion(LocalDateTime ahora, List<List<LogroDesbloqueadoDto>> listas) {}

    @SuppressWarnings("unchecked")
    private static void encolarEnPeticion(RequestAttributes peticion, Map<Integer, Pendiente> nuevos) {
        Map<Integer, PendientePeticion> cola =
                (Map<Integer, PendientePeticion>) peticion.getAttribute(PENDIENTES_PETICION, RequestAttributes.SCOPE_REQUEST);
        if (cola == null) {
            cola = new LinkedHashMap<>();
            peticion.setAttribute(PENDIENTES_PETICION, cola, RequestAttributes.SCOPE_REQUEST);
        }
        for (Map.Entry<Integer, Pendiente> p : nuevos.entrySet()) {
            cola.computeIfAbsent(p.getKey(), id -> new PendientePeticion(p.getValue().ahora(), new ArrayList<>()))
                    .listas().add(p.getValue().lista());
        }
    }

    /**
     * Evalua lo que las acciones de esta peticion dejaron anotado. Lo llaman {@code LogrosTrasLaAccion}
     * (justo antes de escribir el cuerpo, para que logrosNuevos llegue en la respuesta) y
     * {@code LogrosPendientesFilter} (al final, para respuestas sin cuerpo o con error). Si todavia hay una
     * transaccion abierta no hace nada: la evaluacion necesitaria una segunda conexion.
     */
    @SuppressWarnings("unchecked")
    public void evaluarPendientesDeLaPeticion() {
        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        if (peticion == null || TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        Map<Integer, PendientePeticion> cola =
                (Map<Integer, PendientePeticion>) peticion.getAttribute(PENDIENTES_PETICION, RequestAttributes.SCOPE_REQUEST);
        if (cola == null || cola.isEmpty()) {
            return;
        }
        peticion.removeAttribute(PENDIENTES_PETICION, RequestAttributes.SCOPE_REQUEST);
        for (Map.Entry<Integer, PendientePeticion> p : cola.entrySet()) {
            List<LogroDesbloqueadoDto> nuevos = evaluarEnTransaccionPropia(p.getKey(), p.getValue().ahora());
            for (List<LogroDesbloqueadoDto> lista : p.getValue().listas()) {
                lista.addAll(nuevos);
            }
        }
    }

    // ------------------------------------------------------------------ evaluacion

    /**
     * Evalua todos los logros activos del usuario dentro de la transaccion actual. Si algo falla, LANZA
     * (para que su transaccion se deshaga entera); las acciones del juego deben usar {@link #evaluarSeguro}.
     */
    @Transactional
    public List<LogroDesbloqueadoDto> evaluar(Integer userId, LocalDateTime ahora) {
        return evaluar(userId, ahora, MAX_VUELTAS);
    }

    /**
     * {@code vueltasRestantes} limita la cadena "el premio de un logro desbloquea otro" (por ejemplo, el XP
     * sube de nivel y desbloquea NIVEL_5): como mucho {@link #MAX_VUELTAS} vueltas sobre la lista; lo que
     * quede se desbloquea en la siguiente evaluacion. Las vueltas reusan logros, estados y conteos ya leidos
     * (un premio solo cambia XP, monedas y nivel), asi que las consultas no crecen con la cantidad de logros.
     */
    @Transactional
    public List<LogroDesbloqueadoDto> evaluar(Integer userId, LocalDateTime ahora, int vueltasRestantes) {
        // Antes de leer nada: asi nadie mas puede estar desbloqueando (ni pagando) logros de este usuario.
        // Mismo bloqueo que ProgresoService.aplicar y TiendaService.comprar; es reentrante dentro de la misma
        // transaccion y solo toma la fila de ESTE usuario, asi que no crea ciclos de espera nuevos.
        usuarioRepo.bloquear(userId);
        Usuario u = usuarioRepo.findById(userId).orElse(null);
        if (u == null) {
            return List.of();
        }
        List<Logro> activos = logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc();
        Map<Integer, LogroUsuario> estados = estadosPorLogro(userId);
        if (activos.stream().anyMatch(l -> !estados.containsKey(l.getIdLogro()))) {
            // Crea de una vez las filas que falten (ON CONFLICT DO NOTHING) y las vuelve a leer
            logroUsuarioRepo.crearFaltantes(userId);
            estados.clear();
            estados.putAll(estadosPorLogro(userId));
        }
        Evaluacion ev = new Evaluacion();
        List<LogroDesbloqueadoDto> nuevos = new ArrayList<>();
        for (int vuelta = 0; vuelta < Math.max(1, vueltasRestantes); vuelta++) {
            int nivelAntes = valor(u.getNivel(), 1);
            for (Logro logro : activos) {
                LogroUsuario estado = estadoDe(userId, logro, estados);
                if (estado.getDesbloqueadoEn() != null) {
                    continue; // un logro desbloqueado no se revierte ni se vuelve a pagar
                }
                int meta = Math.max(1, valor(logro.getMeta(), 1));
                int progreso = Math.min(Math.max(0, calcularProgreso(u, logro, ahora, ev)), meta);
                boolean cambio = !Integer.valueOf(progreso).equals(estado.getProgreso());
                estado.setProgreso(progreso);
                if (progreso >= meta) {
                    desbloquear(u, logro, estado, ahora, nuevos, ev);
                } else if (cambio || estado.getIdLogroUsuario() == null) {
                    logroUsuarioRepo.save(estado);
                }
            }
            if (valor(u.getNivel(), 1) == nivelAntes) {
                break; // solo el nivel cambia con un premio: si no subio, otra vuelta no desbloquea nada
            }
        }
        return nuevos;
    }

    /** Lo leido una sola vez por evaluacion: conteos por condicion y datos para pagar. */
    private static final class Evaluacion {
        final Map<String, Integer> conteos = new HashMap<>();
        ProgresoService.DatosPremio premio;
    }

    private void desbloquear(Usuario u, Logro logro, LogroUsuario estado, LocalDateTime ahora,
                             List<LogroDesbloqueadoDto> nuevos, Evaluacion ev) {
        estado.setDesbloqueadoEn(ahora);
        estado.setNotificado(false);
        logroUsuarioRepo.save(estado);
        nuevos.add(aDesbloqueado(logro));
        ProgresoService progreso = progresoService.getObject();
        if (ev.premio == null) {
            ev.premio = progreso.datosPremio();
        }
        progreso.pagarLogro(u, logro, ahora, ev.premio);
    }

    private int calcularProgreso(Usuario u, Logro logro, LocalDateTime ahora, Evaluacion ev) {
        String condicion = logro.getCondicion();
        if (condicion == null || !CONDICIONES.contains(condicion)) {
            log.warn("Condición de logro desconocida: {} (logro {})", condicion, logro.getCodigo());
            return 0;
        }
        return switch (condicion) {
            // Contadores de usuarios (V30) y datos del propio usuario: el nivel puede cambiar entre vueltas
            case "RIEGOS_TOTAL" -> valor(u.getRiegosTotal(), 0);
            case "ABONOS_TOTAL" -> valor(u.getAbonosTotal(), 0);
            case "LUZ_TOTAL" -> valor(u.getLuzTotal(), 0);
            case "RACHA_DIAS" -> rachaVigente(u, ahora);
            case "NIVEL" -> valor(u.getNivel(), 1);
            // Consultas a la base: una por cada condicion + parametro distintos en toda la evaluacion
            default -> ev.conteos.computeIfAbsent(condicion + "|" + logro.getParametro(),
                    k -> contar(u.getIdUsuario(), condicion, logro.getParametro()));
        };
    }

    private int contar(Integer userId, String condicion, String parametro) {
        return switch (condicion) {
            case "ESCANEOS_TOTAL" -> (int) escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(userId);
            case "ESPECIES_DISTINTAS" -> (int) escaneoRepo.countDistinctEspeciesByIdUsuario(userId);
            case "DESASTRE_AFRONTADO" -> parametro == null || parametro.isBlank()
                    ? (int) registroDesastreRepo.countByUsuario(userId)
                    : (int) registroDesastreRepo.countByUsuarioAndTipo(userId, parametro);
            case "DESASTRES_TIPOS_DISTINTOS" -> (int) registroDesastreRepo.countTiposDistintosByUsuario(userId);
            case "PLANTAS_REFUGIADAS" -> (int) registroDesastreRepo.countPlantasRefugiadasByUsuario(userId);
            case "EVENTOS_SIN_PERDIDAS" -> (int) registroDesastreRepo.countEventosSinPerdidasByUsuario(userId);
            case "COMPRAS_TIENDA" -> (int) inventarioRepo.countComprasByUsuario(userId);
            case "MACETAS_COMPRADAS" -> (int) inventarioRepo.countComprasByUsuarioYTipo(userId, "MACETA");
            case "INVERNADEROS_COMPRADOS" -> (int) inventarioRepo.countComprasByUsuarioYTipo(userId, "INVERNADERO");
            case "FONDOS_COMPRADOS" -> (int) inventarioRepo.countComprasByUsuarioYTipo(userId, "FONDO");
            case "MISIONES_COMPLETADAS" -> (int) misionUsuarioRepo.countByIdUsuarioAndCompletadaTrue(userId);
            case "ESCANEO_HORA" -> {
                int[] f = franja(parametro);
                yield f[0] < 0 ? 0 : (int) escaneoRepo.countEscaneosEnFranja(userId, f[0], f[1],
                        zonaParaPostgres(zonaServidor), zonaParaPostgres(zonaJugador));
            }
            default -> 0;
        };
    }

    /**
     * Racha ACTUAL (no la maxima: no hay columna para eso). racha_dias solo se corrige cuando el usuario
     * vuelve a hacer algo, asi que si su ultima actividad fue antes de ayer la racha ya esta rota y vale 0:
     * sin esto, un logro de racha nuevo o con la meta rebajada se desbloquearia con una racha que ya se perdio.
     */
    static int rachaVigente(Usuario u, LocalDateTime ahora) {
        int racha = valor(u.getRachaDias(), 0);
        LocalDate ultima = u.getUltimaActividad();
        if (ultima == null || ahora == null) {
            return racha; // sin registro de actividad: se respeta la racha guardada (igual que ProgresoService)
        }
        return ultima.isBefore(ahora.toLocalDate().minusDays(1)) ? 0 : racha;
    }

    static int[] franja(String parametro) {
        if ("MADRUGADA".equals(parametro)) {
            return new int[] {0, 5}; // antes de 06:00
        }
        if ("NOCHE".equals(parametro)) {
            return new int[] {23, 23}; // desde las 23:00
        }
        return new int[] {-1, -1};
    }

    /**
     * Nombre de zona que Postgres entiende en AT TIME ZONE. Los nombres IANA (America/Lima) pasan tal cual;
     * un desfase fijo se escribe en formato POSIX ("<-05>+05"), porque Postgres lee "-05:00" con el signo al reves.
     */
    static String zonaParaPostgres(ZoneId zona) {
        ZoneId normal = zona.normalized();
        if (!(normal instanceof ZoneOffset offset)) {
            return zona.getId();
        }
        int segundos = offset.getTotalSeconds();
        if (segundos == 0) {
            return "UTC";
        }
        int abs = Math.abs(segundos);
        String hhmm = String.format("%02d:%02d", abs / 3600, (abs % 3600) / 60);
        String nombre = (segundos < 0 ? "-" : "+") + hhmm;
        return "<" + nombre + ">" + (segundos < 0 ? "+" : "-") + hhmm;
    }

    ZoneId zonaJugador() {
        return zonaJugador;
    }

    private Map<Integer, LogroUsuario> estadosPorLogro(Integer userId) {
        Map<Integer, LogroUsuario> mapa = new LinkedHashMap<>();
        for (LogroUsuario lu : logroUsuarioRepo.findByIdUsuario(userId)) {
            mapa.put(lu.getIdLogro(), lu);
        }
        return mapa;
    }

    /**
     * Estado del logro para este usuario. Si no estaba en el mapa (p. ej. lo creo una evaluacion anidada
     * despues de cargarlo), se vuelve a buscar antes de crear uno: nunca dos filas ni dos pagos.
     */
    private LogroUsuario estadoDe(Integer userId, Logro logro, Map<Integer, LogroUsuario> estados) {
        return estados.computeIfAbsent(logro.getIdLogro(), id -> logroUsuarioRepo.findByIdUsuarioAndIdLogro(userId, id).orElseGet(() -> {
            LogroUsuario lu = new LogroUsuario();
            lu.setIdUsuario(userId);
            lu.setIdLogro(id);
            lu.setProgreso(0);
            lu.setNotificado(false);
            return lu;
        }));
    }

    // ------------------------------------------------------------------ lectura

    /** Sin @Transactional a proposito: primero evalua en su propia transaccion y luego lee lo ya guardado. */
    public LogrosResponseDto listar(Integer userId, LocalDateTime ahora) {
        evaluarSeguro(userId, ahora);
        List<Logro> todos = logroRepo.findAllByOrderByOrdenAscIdLogroAsc();
        Map<Integer, LogroUsuario> estados = estadosPorLogro(userId);
        List<LogroDto> visibles = new ArrayList<>();
        for (Logro logro : todos) {
            LogroUsuario estado = estados.get(logro.getIdLogro());
            boolean desbloqueado = estado != null && estado.getDesbloqueadoEn() != null;
            boolean activo = Boolean.TRUE.equals(logro.getActivo());
            if (!activo && !desbloqueado) {
                continue; // desactivado: no se ve salvo que ya lo hubiera ganado
            }
            visibles.add(aDto(logro, estado));
        }
        int desbloqueados = (int) visibles.stream().filter(LogroDto::desbloqueado).count();
        return new LogrosResponseDto(visibles.size(), desbloqueados, visibles);
    }

    public List<LogroDesbloqueadoDto> pendientes(Integer userId, LocalDateTime ahora) {
        evaluarSeguro(userId, ahora);
        Map<Integer, Logro> porId = logroRepo.findAll().stream().collect(Collectors.toMap(Logro::getIdLogro, l -> l, (a, b) -> a));
        return logroUsuarioRepo.findByIdUsuarioAndDesbloqueadoEnIsNotNullAndNotificadoFalse(userId).stream()
                .map(LogroUsuario::getIdLogro)
                .map(porId::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing((Logro l) -> valor(l.getOrden(), 0)).thenComparing(Logro::getIdLogro))
                .map(LogroService::aDesbloqueado)
                .toList();
    }

    /**
     * Marca como vistos (ya celebrados) logros desbloqueados del usuario de la sesion con una sola sentencia
     * que filtra por id_usuario: nunca toca filas de otro. Acepta hasta {@link #MAX_VISTOS} ids (400 si se pasa).
     */
    @Transactional
    public void marcarVistos(Integer userId, List<Integer> ids) {
        if (ids != null && ids.size() > MAX_VISTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se pueden marcar como vistos hasta " + MAX_VISTOS + " logros a la vez");
        }
        if (userId == null || ids == null || ids.isEmpty()) {
            return;
        }
        LinkedHashSet<Integer> unicos = new LinkedHashSet<>(ids);
        unicos.remove(null);
        if (!unicos.isEmpty()) {
            logroUsuarioRepo.marcarVistos(userId, unicos);
        }
    }

    /** Codigo que se muestra de un logro secreto aun bloqueado: no revela el real (p. ej. MADRUGADOR). */
    static String codigoSecreto(Logro logro) {
        return "secreto_" + logro.getIdLogro();
    }

    static LogroDto aDto(Logro logro, LogroUsuario estado) {
        boolean desbloqueado = estado != null && estado.getDesbloqueadoEn() != null;
        boolean secretoBloqueado = Boolean.TRUE.equals(logro.getSecreto()) && !desbloqueado;
        int meta = Math.max(1, valor(logro.getMeta(), 1));
        int progreso = estado != null ? Math.min(valor(estado.getProgreso(), 0), meta) : 0;
        if (secretoBloqueado) {
            return new LogroDto(logro.getIdLogro(), codigoSecreto(logro), "???", "Logro secreto", "secreto",
                    RAREZA_SECRETA, 1, 0, false, null, true, 0, 0);
        }
        return new LogroDto(logro.getIdLogro(), logro.getCodigo(), logro.getTitulo(), logro.getDescripcion(),
                logro.getIcono(), logro.getRareza(), meta, desbloqueado ? meta : progreso, desbloqueado,
                estado != null ? estado.getDesbloqueadoEn() : null, Boolean.TRUE.equals(logro.getSecreto()),
                valor(logro.getRecompensaXp(), 0), valor(logro.getRecompensaMonedas(), 0));
    }

    /** Solo se arma para logros ya desbloqueados, asi que puede llevar el codigo real. */
    static LogroDesbloqueadoDto aDesbloqueado(Logro logro) {
        return new LogroDesbloqueadoDto(logro.getIdLogro(), logro.getCodigo(), logro.getTitulo(),
                logro.getDescripcion(), logro.getIcono(), logro.getRareza(),
                valor(logro.getRecompensaXp(), 0), valor(logro.getRecompensaMonedas(), 0));
    }

    private static int valor(Integer n, int defecto) {
        return n != null ? n : defecto;
    }
}
