package com.plantia.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class LogroService {

    private static final Logger log = LoggerFactory.getLogger(LogroService.class);
    static final String MOTIVO_LOGRO = "LOGRO";
    static final int MAX_VUELTAS = 3;

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

    public LogroService(LogroRepository logroRepo,
                        LogroUsuarioRepository logroUsuarioRepo,
                        UsuarioRepository usuarioRepo,
                        EscaneoHistorialRepository escaneoRepo,
                        RegistroDesastreUsuarioRepository registroDesastreRepo,
                        MisionUsuarioRepository misionUsuarioRepo,
                        InventarioUsuarioRepository inventarioRepo,
                        ObjectProvider<ProgresoService> progresoService) {
        this.logroRepo = logroRepo;
        this.logroUsuarioRepo = logroUsuarioRepo;
        this.usuarioRepo = usuarioRepo;
        this.escaneoRepo = escaneoRepo;
        this.registroDesastreRepo = registroDesastreRepo;
        this.misionUsuarioRepo = misionUsuarioRepo;
        this.inventarioRepo = inventarioRepo;
        this.progresoService = progresoService;
    }

    @Transactional
    public List<LogroDesbloqueadoDto> evaluar(Integer userId, LocalDateTime ahora) {
        return evaluar(userId, ahora, MAX_VUELTAS);
    }

    @Transactional
    public List<LogroDesbloqueadoDto> evaluar(Integer userId, LocalDateTime ahora, int vueltasRestantes) {
        try {
            Usuario u = usuarioRepo.findById(userId).orElse(null);
            if (u == null) {
                return List.of();
            }
            List<Logro> activos = logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc();
            Map<Integer, LogroUsuario> estados = estadosPorLogro(userId);
            List<LogroDesbloqueadoDto> nuevos = new ArrayList<>();
            for (Logro logro : activos) {
                LogroUsuario estado = estadoDe(userId, logro, estados);
                if (estado.getDesbloqueadoEn() != null) {
                    continue;
                }
                int progreso = calcularProgreso(u, logro, ahora);
                int meta = Math.max(1, valor(logro.getMeta(), 1));
                estado.setProgreso(Math.min(progreso, meta));
                if (estado.getProgreso() >= meta) {
                    desbloquear(u, logro, estado, ahora, nuevos, vueltasRestantes);
                }
                logroUsuarioRepo.save(estado);
            }
            return nuevos;
        } catch (RuntimeException e) {
            log.error("No se pudieron evaluar logros para el usuario {}", userId, e);
            return List.of();
        }
    }

    private void desbloquear(Usuario u, Logro logro, LogroUsuario estado, LocalDateTime ahora,
                             List<LogroDesbloqueadoDto> nuevos, int vueltasRestantes) {
        estado.setDesbloqueadoEn(ahora);
        estado.setNotificado(false);
        logroUsuarioRepo.save(estado);
        nuevos.add(aDesbloqueado(logro));
        nuevos.addAll(progresoService.getObject().premiarLogro(u, logro, ahora, vueltasRestantes));
    }

    private int calcularProgreso(Usuario u, Logro logro, LocalDateTime ahora) {
        String condicion = logro.getCondicion();
        if (condicion == null || !CONDICIONES.contains(condicion)) {
            log.warn("Condición de logro desconocida: {} (logro {})", condicion, logro.getCodigo());
            return 0;
        }
        Integer userId = u.getIdUsuario();
        String parametro = logro.getParametro();
        return switch (condicion) {
            case "ESCANEOS_TOTAL" -> (int) escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(userId);
            case "ESPECIES_DISTINTAS" -> (int) escaneoRepo.countDistinctEspeciesByIdUsuario(userId);
            case "DESASTRE_AFRONTADO" -> parametro == null || parametro.isBlank()
                    ? (int) registroDesastreRepo.countByUsuario(userId)
                    : (int) registroDesastreRepo.countByUsuarioAndTipo(userId, parametro);
            case "DESASTRES_TIPOS_DISTINTOS" -> (int) registroDesastreRepo.countTiposDistintosByUsuario(userId);
            case "PLANTAS_REFUGIADAS" -> (int) registroDesastreRepo.countPlantasRefugiadasByUsuario(userId);
            case "EVENTOS_SIN_PERDIDAS" -> (int) registroDesastreRepo.countEventosSinPerdidasByUsuario(userId);
            case "RIEGOS_TOTAL" -> (int) progresoService.getObject().contarAccionHistorial(userId, "REGAR");
            case "ABONOS_TOTAL" -> (int) progresoService.getObject().contarAccionHistorial(userId, "ABONAR");
            case "LUZ_TOTAL" -> (int) progresoService.getObject().contarAccionHistorial(userId, "SOL");
            case "RACHA_DIAS" -> Math.max(valor(u.getRachaDias(), 0), diasDesdeUltimaActividad(u, ahora));
            case "NIVEL" -> valor(u.getNivel(), 1);
            case "COMPRAS_TIENDA" -> (int) inventarioRepo.countComprasByUsuario(userId);
            case "MACETAS_COMPRADAS" -> (int) inventarioRepo.countComprasByUsuarioYTipo(userId, "MACETA");
            case "INVERNADEROS_COMPRADOS" -> (int) inventarioRepo.countComprasByUsuarioYTipo(userId, "INVERNADERO");
            case "FONDOS_COMPRADOS" -> (int) inventarioRepo.countComprasByUsuarioYTipo(userId, "FONDO");
            case "MISIONES_COMPLETADAS" -> (int) misionUsuarioRepo.countByIdUsuarioAndCompletadaTrue(userId);
            case "ESCANEO_HORA" -> {
                int[] f = franja(parametro);
                yield f[0] < 0 ? 0 : (int) escaneoRepo.countEscaneosEnFranja(userId, f[0], f[1]);
            }
            default -> 0;
        };
    }

    private static int diasDesdeUltimaActividad(Usuario u, LocalDateTime ahora) {
        // rachaDias ya representa la racha actual; no inventamos otro contador.
        return valor(u.getRachaDias(), 0);
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

    private Map<Integer, LogroUsuario> estadosPorLogro(Integer userId) {
        Map<Integer, LogroUsuario> mapa = new LinkedHashMap<>();
        for (LogroUsuario lu : logroUsuarioRepo.findByIdUsuario(userId)) {
            mapa.put(lu.getIdLogro(), lu);
        }
        return mapa;
    }

    private LogroUsuario estadoDe(Integer userId, Logro logro, Map<Integer, LogroUsuario> estados) {
        return estados.computeIfAbsent(logro.getIdLogro(), id -> {
            LogroUsuario lu = new LogroUsuario();
            lu.setIdUsuario(userId);
            lu.setIdLogro(id);
            lu.setProgreso(0);
            lu.setNotificado(false);
            return lu;
        });
    }

    @Transactional
    public LogrosResponseDto listar(Integer userId, LocalDateTime ahora) {
        evaluar(userId, ahora);
        List<Logro> todos = logroRepo.findAllByOrderByOrdenAscIdLogroAsc();
        Map<Integer, LogroUsuario> estados = estadosPorLogro(userId);
        List<LogroDto> visibles = new ArrayList<>();
        for (Logro logro : todos) {
            LogroUsuario estado = estados.get(logro.getIdLogro());
            boolean desbloqueado = estado != null && estado.getDesbloqueadoEn() != null;
            boolean activo = Boolean.TRUE.equals(logro.getActivo());
            if (!activo && !desbloqueado) {
                continue;
            }
            visibles.add(aDto(logro, estado));
        }
        int desbloqueados = (int) visibles.stream().filter(LogroDto::desbloqueado).count();
        return new LogrosResponseDto(visibles.size(), desbloqueados, visibles);
    }

    @Transactional
    public List<LogroDesbloqueadoDto> pendientes(Integer userId, LocalDateTime ahora) {
        evaluar(userId, ahora);
        Map<Integer, Logro> porId = logroRepo.findAll().stream().collect(Collectors.toMap(Logro::getIdLogro, l -> l, (a, b) -> a));
        return logroUsuarioRepo.findByIdUsuarioAndDesbloqueadoEnIsNotNullAndNotificadoFalse(userId).stream()
                .map(LogroUsuario::getIdLogro)
                .map(porId::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing((Logro l) -> valor(l.getOrden(), 0)).thenComparing(Logro::getIdLogro))
                .map(LogroService::aDesbloqueado)
                .toList();
    }

    @Transactional
    public void marcarVistos(Integer userId, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Integer idLogro : ids) {
            if (idLogro == null) {
                continue;
            }
            logroUsuarioRepo.findByIdUsuarioAndIdLogro(userId, idLogro).ifPresent(estado -> {
                if (estado.getDesbloqueadoEn() != null && !Boolean.TRUE.equals(estado.getNotificado())) {
                    estado.setNotificado(true);
                    logroUsuarioRepo.save(estado);
                }
            });
        }
    }

    static LogroDto aDto(Logro logro, LogroUsuario estado) {
        boolean desbloqueado = estado != null && estado.getDesbloqueadoEn() != null;
        boolean secretoBloqueado = Boolean.TRUE.equals(logro.getSecreto()) && !desbloqueado;
        int meta = Math.max(1, valor(logro.getMeta(), 1));
        int progreso = estado != null ? Math.min(valor(estado.getProgreso(), 0), meta) : 0;
        if (secretoBloqueado) {
            return new LogroDto(logro.getIdLogro(), logro.getCodigo(), "???", "Logro secreto", "secreto",
                    logro.getRareza(), 1, 0, false, null, true, 0, 0);
        }
        return new LogroDto(logro.getIdLogro(), logro.getCodigo(), logro.getTitulo(), logro.getDescripcion(),
                logro.getIcono(), logro.getRareza(), meta, desbloqueado ? meta : progreso, desbloqueado,
                estado != null ? estado.getDesbloqueadoEn() : null, Boolean.TRUE.equals(logro.getSecreto()),
                valor(logro.getRecompensaXp(), 0), valor(logro.getRecompensaMonedas(), 0));
    }

    static LogroDesbloqueadoDto aDesbloqueado(Logro logro) {
        return new LogroDesbloqueadoDto(logro.getIdLogro(), logro.getCodigo(), logro.getTitulo(),
                logro.getDescripcion(), logro.getIcono(), logro.getRareza(),
                valor(logro.getRecompensaXp(), 0), valor(logro.getRecompensaMonedas(), 0));
    }

    private static int valor(Integer n, int defecto) {
        return n != null ? n : defecto;
    }
}
