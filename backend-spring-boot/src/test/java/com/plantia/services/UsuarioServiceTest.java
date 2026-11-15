package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.plantia.dto.UserSummaryDto;
import com.plantia.models.Nivel;
import com.plantia.models.Usuario;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.UsuarioRepository;

/** El perfil trae los limites del nivel actual para dibujar la barra de XP. */
class UsuarioServiceTest {

    private NivelRepository nivelRepo;
    private UsuarioRepository usuarioRepo;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        nivelRepo = mock(NivelRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        when(nivelRepo.findById(2)).thenReturn(Optional.of(nivel(2, 500)));
        when(nivelRepo.findById(3)).thenReturn(Optional.of(nivel(3, 1000)));
        service = new UsuarioService(usuarioRepo, nivelRepo);
    }

    private static Nivel nivel(int n, int xp) {
        Nivel nv = new Nivel();
        nv.setNivel(n);
        nv.setXpRequerido(xp);
        return nv;
    }

    private void usuario(int nivel, int xp) {
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombre("Johan");
        u.setNivel(nivel);
        u.setExperienciaXp(xp);
        u.setRachaDias(3);
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(u));
    }

    @Test
    void enMitadDeNivelDaLosDosLimites() {
        usuario(2, 620);

        UserSummaryDto dto = service.me(1);

        assertThat(dto.experienceFromLevel()).isEqualTo(500);
        assertThat(dto.experienceToNextLevel()).isEqualTo(1000);
        assertThat(dto.experience()).isEqualTo(620);
        assertThat(dto.streakDays()).isEqualTo(3);
    }

    @Test
    void enElUltimoNivelLosDosLimitesSeIgualanAlXpYLaBarraQuedaCompleta() {
        when(nivelRepo.findById(4)).thenReturn(Optional.empty());
        when(nivelRepo.findById(3)).thenReturn(Optional.of(nivel(3, 1000)));
        usuario(3, 1250);

        UserSummaryDto dto = service.me(1);

        assertThat(dto.experienceToNextLevel()).isEqualTo(1250);
        assertThat(dto.experienceFromLevel()).isEqualTo(1000);
    }

    @Test
    void conUnNivelQueNoEstaEnLaTablaLaBaseEsCero() {
        usuario(1, 120);

        UserSummaryDto dto = service.me(1);

        assertThat(dto.experienceFromLevel()).isZero();
    }
}
