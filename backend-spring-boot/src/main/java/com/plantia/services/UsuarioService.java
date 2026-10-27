package com.plantia.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.UserSummaryDto;
import com.plantia.models.Usuario;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final NivelRepository nivelRepo;

    public UsuarioService(UsuarioRepository usuarioRepo, NivelRepository nivelRepo) {
        this.usuarioRepo = usuarioRepo;
        this.nivelRepo = nivelRepo;
    }

    @Transactional(readOnly = true)
    public UserSummaryDto me(Integer userId) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        int nivel = u.getNivel() != null ? u.getNivel() : 1;
        int xp = u.getExperienciaXp() != null ? u.getExperienciaXp() : 0;
        int siguiente = nivelRepo.findById(nivel + 1).map(n -> n.getXpRequerido()).orElse(xp + 500);

        return new UserSummaryDto(
                u.getNombre(),
                nivel,
                badge(nivel),
                u.getMonedas() != null ? u.getMonedas() : 0,
                xp,
                siguiente,
                null);
    }

    private static String badge(int nivel) {
        if (nivel >= 10) {
            return "Maestro Botanico";
        }
        if (nivel >= 5) {
            return "Cuidador";
        }
        return "Aprendiz";
    }
}
