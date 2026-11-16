package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.WeatherAlertDto;
import com.plantia.models.EventoDesastre;
import com.plantia.models.TipoDesastre;
import com.plantia.models.Usuario;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.TipoDesastreRepository;
import com.plantia.repositories.UsuarioRepository;

@Service
public class EventoService {

    private final EventoDesastreRepository eventoRepo;
    private final UsuarioRepository usuarioRepo;
    private final TipoDesastreRepository tipoRepo;

    public EventoService(EventoDesastreRepository eventoRepo, UsuarioRepository usuarioRepo,
                         TipoDesastreRepository tipoRepo) {
        this.eventoRepo = eventoRepo;
        this.usuarioRepo = usuarioRepo;
        this.tipoRepo = tipoRepo;
    }

    @Transactional(readOnly = true)
    public Optional<WeatherAlertDto> activo(Integer userId) {
        String region = usuarioRepo.findById(userId)
                .map(Usuario::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .orElse("GLOBAL");

        return eventoRepo.activosParaRegion(region).stream()
                .findFirst()
                .map(this::toDto);
    }

    private WeatherAlertDto toDto(EventoDesastre e) {
        TipoDesastre tipo = e.getIdTipoDesastre() == null ? null : tipoRepo.findById(e.getIdTipoDesastre()).orElse(null);
        LocalDateTime ahora = LocalDateTime.now();
        return new WeatherAlertDto(
                titulo(e.getTipoEvento()),
                e.getMensajeNotificacion(),
                severidad(e.getSeveridad()),
                restante(e.getFechaFin()),
                "Prevenir evento",
                e.getTipoEvento(),
                fase(e.getFechaInicio(), e.getFechaFin(), ahora),
                tipo != null ? tipo.getNombre() : null,
                tipo != null ? tipo.getImagenUrl() : null,
                tipo != null ? tipo.getDescripcion() : null,
                tipo != null ? tipo.getConsejo() : null,
                e.getFechaFin() == null ? null : Math.max(0, Duration.between(ahora, e.getFechaFin()).getSeconds()));
    }

    /** Fraccion del evento que debe haber pasado para que la animacion deje de ser un aviso. */
    static final double FRACCION_AVISO = 0.4;

    /** AVISO durante el primer tramo del evento; PICO despues (y cuando no hay fechas fiables). */
    static String fase(LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            return "PICO";
        }
        double total = Duration.between(inicio, fin).toMillis();
        double pasado = Duration.between(inicio, ahora).toMillis();
        return pasado < total * FRACCION_AVISO ? "AVISO" : "PICO";
    }

    private static String severidad(String s) {
        if (s == null) {
            return "MEDIUM";
        }
        return switch (s) {
            case "CRITICO" -> "HIGH";
            case "BAJO" -> "LOW";
            default -> "MEDIUM";
        };
    }

    private static String titulo(String tipo) {
        if (tipo == null) {
            return "Alerta climatica";
        }
        return switch (tipo) {
            case "HUAYCO" -> "Se aproxima un Huayco Inminente";
            case "HELADA" -> "Helada Inminente";
            case "OLA_DE_CALOR" -> "Ola de Calor Inminente";
            case "PLAGA" -> "Plaga Detectada";
            default -> "Alerta climatica";
        };
    }

    private static String restante(LocalDateTime fin) {
        if (fin == null) {
            return "";
        }
        long minutos = Math.max(0, Duration.between(LocalDateTime.now(), fin).toMinutes());
        return String.format("%02dh %02dm", minutos / 60, minutos % 60);
    }
}
