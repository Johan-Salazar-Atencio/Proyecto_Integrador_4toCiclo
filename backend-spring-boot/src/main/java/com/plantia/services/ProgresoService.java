package com.plantia.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.ProgresoDto;
import com.plantia.models.HistorialProgreso;
import com.plantia.models.MisionUsuario;
import com.plantia.models.Nivel;
import com.plantia.models.ReglaProgreso;
import com.plantia.models.Usuario;
import com.plantia.repositories.HistorialProgresoRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.UsuarioRepository;

/**
 * Único lugar donde se suma XP y monedas: aplica las reglas de reglas_progreso, sube de nivel
 * con la tabla niveles (xp_requerido es el XP total para alcanzar ese nivel) y mantiene la racha
 * diaria. Cada ganancia queda en historial_progreso.
 */
@Service
public class ProgresoService {

    static final String MOTIVO_RACHA = "RACHA";
    static final String MOTIVO_MISION = "MISION";

    private final UsuarioRepository usuarioRepo;
    private final NivelRepository nivelRepo;
    private final ReglaProgresoRepository reglaRepo;
    private final HistorialProgresoRepository historialRepo;
    private final MisionService misionService;
    private final MisionUsuarioRepository misionUsuarioRepo;

    public ProgresoService(UsuarioRepository usuarioRepo,
                           NivelRepository nivelRepo,
                           ReglaProgresoRepository reglaRepo,
                           HistorialProgresoRepository historialRepo,
                           MisionService misionService,
                           MisionUsuarioRepository misionUsuarioRepo) {
        this.usuarioRepo = usuarioRepo;
        this.nivelRepo = nivelRepo;
        this.reglaRepo = reglaRepo;
        this.historialRepo = historialRepo;
        this.misionService = misionService;
        this.misionUsuarioRepo = misionUsuarioRepo;
    }

    @Transactional
    public ProgresoDto otorgar(Integer userId, String motivo, Integer idReferencia) {
        return otorgar(userId, List.of(motivo), idReferencia, LocalDateTime.now());
    }

    @Transactional
    public ProgresoDto otorgar(Integer userId, List<String> motivos, Integer idReferencia) {
        return otorgar(userId, motivos, idReferencia, LocalDateTime.now());
    }

    /**
     * Aplica varias reglas de una sola acción (por ejemplo ESCANEO y ESPECIE_NUEVA) y la racha.
     * Una regla desconocida, inactiva o que ya llegó a su tope diario no suma, pero no falla.
     */
    @Transactional
    public ProgresoDto otorgar(Integer userId, List<String> motivos, Integer idReferencia, LocalDateTime ahora) {
        return aplicar(userId, motivos, List.of(), idReferencia, ahora, true);
    }

    /**
     * Igual que {@link #otorgar(Integer, List, Integer)}, y además cuenta la acción en las misiones
     * activas del día cuyo tipo coincide (RIEGO, ESCANEO, DIGITALIZAR, COLECCION, SOL, ABONO).
     * Al completar una misión se suma su recompensa de XP y monedas, una sola vez por día.
     */
    @Transactional
    public ProgresoDto otorgar(Integer userId, List<String> motivos, List<String> acciones, Integer idReferencia) {
        return aplicar(userId, motivos, acciones, idReferencia, LocalDateTime.now(), true);
    }

    /**
     * Cuenta solo una acción en las misiones (por ejemplo REFUGIO, cuya recompensa de regla llega
     * cuando termina el evento): no suma reglas, no cuenta como actividad del día ni toca la racha.
     */
    @Transactional
    public ProgresoDto registrarAccion(Integer userId, String accion, Integer idReferencia, LocalDateTime ahora) {
        return aplicar(userId, List.of(), List.of(accion), idReferencia, ahora, false);
    }

    /**
     * Premio que el usuario no provoca con una acción suya (salvar una planta cuando termina un
     * desastre): suma XP, pero no cuenta como actividad del día ni toca la racha.
     */
    @Transactional
    public ProgresoDto otorgarSinRacha(Integer userId, String motivo, Integer idReferencia, LocalDateTime ahora) {
        return aplicar(userId, List.of(motivo), List.of(), idReferencia, ahora, false);
    }

    private ProgresoDto aplicar(Integer userId, List<String> motivos, List<String> acciones,
                                Integer idReferencia, LocalDateTime ahora, boolean conRacha) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        List<Nivel> niveles = nivelRepo.findAll().stream()
                .sorted(Comparator.comparing(Nivel::getNivel))
                .toList();
        int nivelInicial = valor(u.getNivel(), 1);

        List<String> aplicar = new ArrayList<>();
        if (conRacha && actualizarRacha(u, ahora.toLocalDate())) {
            aplicar.add(MOTIVO_RACHA);
        }
        aplicar.addAll(motivos);

        int xpGanado = 0;
        int monedasGanadas = 0;
        for (String motivo : aplicar) {
            ReglaProgreso regla = reglaRepo.findByMotivo(motivo).filter(r -> Boolean.TRUE.equals(r.getActiva())).orElse(null);
            if (regla == null || topeAlcanzado(u.getIdUsuario(), regla, ahora)) {
                continue;
            }
            int[] ganado = sumar(u, regla, niveles);
            xpGanado += ganado[0];
            monedasGanadas += ganado[1];
            if (ganado[0] != 0 || ganado[1] != 0) {
                historialRepo.save(movimiento(u, regla, ganado, idReferencia, ahora));
            }
        }
        if (!acciones.isEmpty()) {
            int[] premio = avanzarMisiones(u, acciones, niveles, ahora);
            xpGanado += premio[0];
            monedasGanadas += premio[1];
        }
        u.setUpdatedAt(ahora);
        usuarioRepo.save(u);

        int nivel = valor(u.getNivel(), 1);
        return new ProgresoDto(xpGanado, monedasGanadas, nivel, nivel > nivelInicial,
                valor(u.getExperienciaXp(), 0), valor(u.getRachaDias(), 0));
    }

    /**
     * Suma 1 al avance de cada misión activa de hoy que coincide con alguna acción y, al llegar a
     * la meta, la marca completada y paga su recompensa. Devuelve {xp, monedas} ganados.
     */
    private int[] avanzarMisiones(Usuario u, List<String> acciones, List<Nivel> niveles, LocalDateTime ahora) {
        int xp = 0;
        int monedas = 0;
        ReglaProgreso reglaMision = reglaRepo.findByMotivo(MOTIVO_MISION).orElse(null);
        for (String accion : acciones) {
            for (MisionService.Asignada a : misionService.asignadas(u.getIdUsuario(), ahora.toLocalDate())) {
                MisionUsuario estado = a.estado();
                if (!accion.equals(a.mision().getTipo()) || Boolean.TRUE.equals(estado.getCompletada())) {
                    continue;
                }
                int meta = Math.max(1, valor(a.mision().getMeta(), 1));
                estado.setProgreso(Math.min(meta, valor(estado.getProgreso(), 0) + 1));
                if (estado.getProgreso() >= meta) {
                    estado.setCompletada(true);
                    ReglaProgreso premio = new ReglaProgreso();
                    premio.setXp(valor(a.mision().getRecompensaXp(), 0));
                    premio.setMonedas(valor(a.mision().getRecompensaMonedas(), 0));
                    int[] ganado = sumar(u, premio, niveles);
                    xp += ganado[0];
                    monedas += ganado[1];
                    if (reglaMision != null && (ganado[0] != 0 || ganado[1] != 0)) {
                        historialRepo.save(movimiento(u, reglaMision, ganado, a.mision().getIdMision(), ahora));
                    }
                }
                misionUsuarioRepo.save(estado);
            }
        }
        return new int[] {xp, monedas};
    }

    /** true si hoy es un día nuevo de actividad (suma a la racha); false si ya había actividad hoy. */
    private static boolean actualizarRacha(Usuario u, LocalDate hoy) {
        LocalDate ultima = u.getUltimaActividad();
        if (hoy.equals(ultima)) {
            return false;
        }
        int racha;
        if (ultima == null) {
            // Primera vez que se registra actividad: se respeta la racha que ya tenía el usuario
            racha = Math.max(1, valor(u.getRachaDias(), 0));
        } else if (ultima.plusDays(1).equals(hoy)) {
            racha = valor(u.getRachaDias(), 0) + 1;
        } else {
            racha = 1;
        }
        u.setRachaDias(racha);
        u.setUltimaActividad(hoy);
        return true;
    }

    private boolean topeAlcanzado(Integer userId, ReglaProgreso regla, LocalDateTime ahora) {
        if (regla.getTopeDiario() == null) {
            return false;
        }
        long hoy = historialRepo.countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(
                userId, regla.getIdRegla(), ahora.toLocalDate().atStartOfDay());
        return hoy >= regla.getTopeDiario();
    }

    /** Suma la regla al usuario y sube de nivel las veces que haga falta; devuelve {xp, monedas}. */
    private static int[] sumar(Usuario u, ReglaProgreso regla, List<Nivel> niveles) {
        int xp = valor(regla.getXp(), 0);
        int monedas = valor(regla.getMonedas(), 0);
        u.setExperienciaXp(valor(u.getExperienciaXp(), 0) + xp);
        for (Nivel siguiente : niveles) {
            if (siguiente.getNivel() > valor(u.getNivel(), 1) && u.getExperienciaXp() >= siguiente.getXpRequerido()) {
                u.setNivel(siguiente.getNivel());
                monedas += valor(siguiente.getRecompensaMonedas(), 0);
            }
        }
        u.setMonedas(valor(u.getMonedas(), 0) + monedas);
        return new int[] {xp, monedas};
    }

    private static HistorialProgreso movimiento(Usuario u, ReglaProgreso regla, int[] ganado,
                                                Integer idReferencia, LocalDateTime ahora) {
        HistorialProgreso h = new HistorialProgreso();
        h.setIdUsuario(u.getIdUsuario());
        h.setIdRegla(regla.getIdRegla());
        h.setXpGanado(ganado[0]);
        h.setMonedasGanadas(ganado[1]);
        h.setNivelResultante(valor(u.getNivel(), 1));
        h.setIdReferencia(idReferencia);
        h.setFecha(ahora);
        return h;
    }

    private static int valor(Integer n, int porDefecto) {
        return n != null ? n : porDefecto;
    }
}
