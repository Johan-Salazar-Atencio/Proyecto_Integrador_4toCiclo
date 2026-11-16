package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.DailyMissionDto;
import com.plantia.models.EventoDesastre;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.ReglaProgreso;
import com.plantia.models.TipoDesastre;
import com.plantia.models.Usuario;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.TipoDesastreRepository;
import com.plantia.repositories.UsuarioRepository;

/**
 * La mision del evento activo (tarjeta roja en Misiones): prevenir el evento en todas las plantas vivas del
 * usuario. No es una fila de la tabla misiones: nace con el evento y desaparece cuando termina.
 * La recompensa es la de la regla EVENTO_COMPLETO, que se paga al terminar el evento.
 */
@Service
public class MisionEventoService {

    private final EventoDesastreRepository eventoRepo;
    private final UsuarioRepository usuarioRepo;
    private final PlantaUsuarioRepository plantaRepo;
    private final TipoDesastreRepository tipoRepo;
    private final ReglaProgresoRepository reglaRepo;

    public MisionEventoService(EventoDesastreRepository eventoRepo, UsuarioRepository usuarioRepo,
                               PlantaUsuarioRepository plantaRepo, TipoDesastreRepository tipoRepo,
                               ReglaProgresoRepository reglaRepo) {
        this.eventoRepo = eventoRepo;
        this.usuarioRepo = usuarioRepo;
        this.plantaRepo = plantaRepo;
        this.tipoRepo = tipoRepo;
        this.reglaRepo = reglaRepo;
    }

    @Transactional(readOnly = true)
    public Optional<DailyMissionDto> deEvento(Integer userId) {
        return deEvento(userId, LocalDateTime.now());
    }

    Optional<DailyMissionDto> deEvento(Integer userId, LocalDateTime ahora) {
        String region = usuarioRepo.findById(userId)
                .map(Usuario::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .orElse("GLOBAL");
        Optional<EventoDesastre> activo = eventoRepo.activosParaRegion(region).stream().findFirst();
        if (activo.isEmpty()) {
            return Optional.empty();
        }
        List<PlantaUsuario> vivas = plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(userId).stream()
                .filter(p -> !"MUERTA".equals(p.getEtapaCrecimiento()))
                .toList();
        if (vivas.isEmpty()) {
            return Optional.empty();
        }
        EventoDesastre e = activo.get();
        int total = vivas.size();
        int prevenidas = (int) vivas.stream().filter(p -> Boolean.TRUE.equals(p.getEstaRefugiada())).count();
        ReglaProgreso premio = reglaRepo.findByMotivo("EVENTO_COMPLETO").orElse(null);
        int xp = premio != null && premio.getXp() != null ? premio.getXp() : 0;
        int hojas = premio != null && premio.getMonedas() != null ? premio.getMonedas() : 0;
        long segundos = e.getFechaFin() == null ? 0 : Math.max(0, Duration.between(ahora, e.getFechaFin()).getSeconds());
        return Optional.of(new DailyMissionDto(
                "event_" + e.getIdDesastre(),
                "Prevenir " + nombre(e),
                "+" + xp + " XP · +" + hojas + " hojas",
                Math.min(1f, (float) prevenidas / total),
                prevenidas,
                total,
                prevenidas >= total,
                "EVENT",
                e.getTipoEvento(),
                segundos,
                null));
    }

    private String nombre(EventoDesastre e) {
        String nombre = e.getIdTipoDesastre() == null ? null
                : tipoRepo.findById(e.getIdTipoDesastre()).map(TipoDesastre::getNombre).orElse(null);
        if (nombre == null) {
            nombre = e.getTipoEvento() == null ? "el evento" : e.getTipoEvento().replace('_', ' ').toLowerCase();
        }
        return nombre.substring(0, 1).toLowerCase() + nombre.substring(1);
    }
}
