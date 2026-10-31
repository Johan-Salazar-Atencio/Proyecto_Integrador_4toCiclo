package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.dto.WeatherAlertDto;
import com.plantia.models.EventoDesastre;
import com.plantia.models.Usuario;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.UsuarioRepository;

@Service
public class EventoService {

    private final EventoDesastreRepository eventoRepo;
    private final UsuarioRepository usuarioRepo;

    public EventoService(EventoDesastreRepository eventoRepo, UsuarioRepository usuarioRepo) {
        this.eventoRepo = eventoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional(readOnly = true)
    public Optional<WeatherAlertDto> activo(Integer userId) {
        String region = usuarioRepo.findById(userId)
                .map(Usuario::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .orElse("GLOBAL");

        return eventoRepo.activosParaRegion(region).stream()
                .findFirst()
                .map(EventoService::toDto);
    }

    private static WeatherAlertDto toDto(EventoDesastre e) {
        return new WeatherAlertDto(
                titulo(e.getTipoEvento()),
                e.getMensajeNotificacion(),
                severidad(e.getSeveridad()),
                restante(e.getFechaFin()),
                "Refugiar Planta");
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
