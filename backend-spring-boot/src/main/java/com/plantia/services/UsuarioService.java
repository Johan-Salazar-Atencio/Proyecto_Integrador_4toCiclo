package com.plantia.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.RegionDto;
import com.plantia.dto.UserSummaryDto;
import com.plantia.models.Region;
import com.plantia.models.Usuario;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.RegionRepository;
import com.plantia.repositories.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final NivelRepository nivelRepo;
    private final RegionRepository regionRepo;

    public UsuarioService(UsuarioRepository usuarioRepo, NivelRepository nivelRepo, RegionRepository regionRepo) {
        this.usuarioRepo = usuarioRepo;
        this.nivelRepo = nivelRepo;
        this.regionRepo = regionRepo;
    }

    @Transactional(readOnly = true)
    public UserSummaryDto me(Integer userId) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return resumen(u);
    }

    /** Regiones que el usuario puede elegir en su perfil. */
    @Transactional(readOnly = true)
    public List<RegionDto> regiones() {
        return regionRepo.findByActivaTrueOrderByNombreAsc().stream()
                .map(r -> new RegionDto(r.getCodigo(), r.getNombre()))
                .toList();
    }

    /**
     * Cambia la region del usuario, que decide que desastres le llegan. Solo acepta regiones activas
     * de la tabla regiones (las crea o desactiva el admin). Guarda el codigo y la llave a la vez.
     */
    @Transactional
    public UserSummaryDto cambiarRegion(Integer userId, String codigo) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Region region = regionRepo.findByCodigoAndActivaTrue(codigo == null ? "" : codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Region no valida"));
        u.setRegion(region.getCodigo());
        u.setIdRegion(region.getIdRegion());
        usuarioRepo.save(u);
        return resumen(u);
    }

    private UserSummaryDto resumen(Usuario u) {
        int nivel = u.getNivel() != null ? u.getNivel() : 1;
        int xp = u.getExperienciaXp() != null ? u.getExperienciaXp() : 0;
        // XP total que marca el inicio del nivel actual (0 si el nivel no esta en la tabla)
        int base = nivelRepo.findById(nivel).map(n -> n.getXpRequerido()).orElse(0);
        // En el ultimo nivel no hay siguiente: ambos limites quedan en el XP y la barra se ve completa
        int siguiente = nivelRepo.findById(nivel + 1).map(n -> n.getXpRequerido()).orElse(Math.max(xp, base));

        String email = u.getEmail();
        int streakDays = u.getRachaDias() != null ? u.getRachaDias() : 0;
        String region = u.getRegion() != null && !u.getRegion().isBlank() ? u.getRegion() : "GLOBAL";

        return new UserSummaryDto(
                u.getNombre(),
                nivel,
                badge(nivel),
                u.getMonedas() != null ? u.getMonedas() : 0,
                xp,
                siguiente,
                u.getFotoUrl(),
                email,
                streakDays,
                base,
                region);
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
