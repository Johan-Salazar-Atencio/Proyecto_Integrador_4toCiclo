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
import com.plantia.models.Nivel;
import com.plantia.models.ReglaProgreso;
import com.plantia.models.Usuario;
import com.plantia.repositories.HistorialProgresoRepository;
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

    private final UsuarioRepository usuarioRepo;
    private final NivelRepository nivelRepo;
    private final ReglaProgresoRepository reglaRepo;
    private final HistorialProgresoRepository historialRepo;

    public ProgresoService(UsuarioRepository usuarioRepo,
                           NivelRepository nivelRepo,
                           ReglaProgresoRepository reglaRepo,
                           HistorialProgresoRepository historialRepo) {
        this.usuarioRepo = usuarioRepo;
        this.nivelRepo = nivelRepo;
        this.reglaRepo = reglaRepo;
        this.historialRepo = historialRepo;
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
        return aplicar(userId, motivos, idReferencia, ahora, true);
    }

    /**
     * Premio que el usuario no provoca con una acción suya (salvar una planta cuando termina un
     * desastre): suma XP, pero no cuenta como actividad del día ni toca la racha.
     */
    @Transactional
    public ProgresoDto otorgarSinRacha(Integer userId, String motivo, Integer idReferencia, LocalDateTime ahora) {
        return aplicar(userId, List.of(motivo), idReferencia, ahora, false);
    }

    private ProgresoDto aplicar(Integer userId, List<String> motivos, Integer idReferencia,
                                LocalDateTime ahora, boolean conRacha) {
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
        u.setUpdatedAt(ahora);
        usuarioRepo.save(u);

        int nivel = valor(u.getNivel(), 1);
        return new ProgresoDto(xpGanado, monedasGanadas, nivel, nivel > nivelInicial,
                valor(u.getExperienciaXp(), 0), valor(u.getRachaDias(), 0));
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
