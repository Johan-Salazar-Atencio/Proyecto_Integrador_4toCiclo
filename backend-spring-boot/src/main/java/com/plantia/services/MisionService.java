package com.plantia.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.DailyMissionDto;
import com.plantia.models.Mision;
import com.plantia.models.MisionUsuario;
import com.plantia.repositories.MisionRepository;
import com.plantia.repositories.MisionUsuarioRepository;

@Service
public class MisionService {

    private final MisionRepository misionRepo;
    private final MisionUsuarioRepository misionUsuarioRepo;

    public MisionService(MisionRepository misionRepo, MisionUsuarioRepository misionUsuarioRepo) {
        this.misionRepo = misionRepo;
        this.misionUsuarioRepo = misionUsuarioRepo;
    }

    @Transactional
    public Optional<DailyMissionDto> diaria(Integer userId) {
        List<Mision> activas = misionRepo.findByActivaTrue();
        if (activas.isEmpty()) {
            return Optional.empty();
        }

        Mision mision = activas.get(0);
        LocalDate hoy = LocalDate.now();

        MisionUsuario asignada = misionUsuarioRepo.findByIdUsuarioAndFecha(userId, hoy).stream()
                .filter(mu -> mu.getIdMision().equals(mision.getIdMision()))
                .findFirst()
                .orElseGet(() -> {
                    MisionUsuario nueva = new MisionUsuario();
                    nueva.setIdUsuario(userId);
                    nueva.setIdMision(mision.getIdMision());
                    nueva.setProgreso(0);
                    nueva.setCompletada(false);
                    nueva.setFecha(hoy);
                    return misionUsuarioRepo.save(nueva);
                });

        int progreso = asignada.getProgreso() != null ? asignada.getProgreso() : 0;
        int meta = mision.getMeta() != null && mision.getMeta() > 0 ? mision.getMeta() : 1;
        float avance = Math.min(1f, (float) progreso / meta);
        int recompensa = mision.getRecompensaMonedas() != null ? mision.getRecompensaMonedas() : 0;

        return Optional.of(new DailyMissionDto(
                "mission_" + mision.getIdMision(),
                mision.getTitulo(),
                "+" + recompensa + " gotas de rocio",
                avance,
                progreso,
                meta,
                Boolean.TRUE.equals(asignada.getCompletada())));
    }
}
