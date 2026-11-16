package com.plantia.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.DailyMissionDto;
import com.plantia.models.Mision;
import com.plantia.models.MisionUsuario;
import com.plantia.repositories.MisionRepository;
import com.plantia.repositories.MisionUsuarioRepository;

/**
 * Misiones del día. El admin decide cuáles están activas (tabla misiones); cada usuario recibe
 * todas las activas con su propio progreso. El avance lo suma ProgresoService al registrar acciones.
 */
@Service
public class MisionService {

    /** Una misión activa junto al avance del usuario hoy. */
    public record Asignada(Mision mision, MisionUsuario estado) {
    }

    private final MisionRepository misionRepo;
    private final MisionUsuarioRepository misionUsuarioRepo;

    public MisionService(MisionRepository misionRepo, MisionUsuarioRepository misionUsuarioRepo) {
        this.misionRepo = misionRepo;
        this.misionUsuarioRepo = misionUsuarioRepo;
    }

    /** Misiones activas de hoy para el usuario; crea el avance de las que aún no lo tienen. */
    @Transactional
    public List<Asignada> asignadas(Integer userId, LocalDate hoy) {
        List<Mision> activas = misionRepo.findByActivaTrueOrderByOrdenAscIdMisionAsc();
        List<MisionUsuario> existentes = misionUsuarioRepo.findByIdUsuarioAndFecha(userId, hoy);
        List<Asignada> resultado = new ArrayList<>();
        for (Mision mision : activas) {
            MisionUsuario estado = existentes.stream()
                    .filter(mu -> mu.getIdMision().equals(mision.getIdMision()))
                    .findFirst()
                    .orElseGet(() -> misionUsuarioRepo.save(nueva(userId, mision, hoy)));
            resultado.add(new Asignada(mision, estado));
        }
        return resultado;
    }

    /** Todas las misiones activas de hoy, en el orden que fijó el admin. */
    @Transactional
    public List<DailyMissionDto> deHoy(Integer userId) {
        return asignadas(userId, LocalDate.now()).stream().map(MisionService::aDto).toList();
    }

    /**
     * Una sola misión para la tarjeta del Home de versiones que aún no muestran la lista:
     * la primera pendiente, o la primera si ya están todas completas. 204 si no hay activas.
     */
    @Transactional
    public Optional<DailyMissionDto> diaria(Integer userId) {
        List<DailyMissionDto> hoy = deHoy(userId);
        return hoy.stream().filter(m -> !m.isCompleted()).findFirst()
                .or(() -> hoy.stream().findFirst());
    }

    private static MisionUsuario nueva(Integer userId, Mision mision, LocalDate hoy) {
        MisionUsuario nueva = new MisionUsuario();
        nueva.setIdUsuario(userId);
        nueva.setIdMision(mision.getIdMision());
        nueva.setProgreso(0);
        nueva.setCompletada(false);
        nueva.setFecha(hoy);
        return nueva;
    }

    static DailyMissionDto aDto(Asignada a) {
        Mision mision = a.mision();
        int progreso = a.estado().getProgreso() != null ? a.estado().getProgreso() : 0;
        int meta = mision.getMeta() != null && mision.getMeta() > 0 ? mision.getMeta() : 1;
        float avance = Math.min(1f, (float) progreso / meta);
        int xp = mision.getRecompensaXp() != null ? mision.getRecompensaXp() : 0;
        int monedas = mision.getRecompensaMonedas() != null ? mision.getRecompensaMonedas() : 0;
        return new DailyMissionDto(
                "mission_" + mision.getIdMision(),
                mision.getTitulo(),
                "+" + xp + " XP · +" + monedas + " hojas",
                avance,
                Math.min(progreso, meta),
                meta,
                Boolean.TRUE.equals(a.estado().getCompletada()));
    }
}
