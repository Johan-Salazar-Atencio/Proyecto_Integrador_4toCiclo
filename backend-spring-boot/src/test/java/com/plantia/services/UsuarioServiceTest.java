package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.RegionDto;
import com.plantia.dto.UserSummaryDto;
import com.plantia.models.Nivel;
import com.plantia.models.Region;
import com.plantia.models.Usuario;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.RegionRepository;
import com.plantia.repositories.UsuarioRepository;

/** El perfil trae los limites del nivel actual para dibujar la barra de XP. */
class UsuarioServiceTest {

    private NivelRepository nivelRepo;
    private UsuarioRepository usuarioRepo;
    private RegionRepository regionRepo;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        nivelRepo = mock(NivelRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        when(nivelRepo.findById(2)).thenReturn(Optional.of(nivel(2, 500)));
        when(nivelRepo.findById(3)).thenReturn(Optional.of(nivel(3, 1000)));

        regionRepo = mock(RegionRepository.class);
        service = new UsuarioService(usuarioRepo, nivelRepo, regionRepo);
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

    private static Region region(int id, String codigo, String nombre) {
        Region r = new Region();
        r.setIdRegion(id);
        r.setCodigo(codigo);
        r.setNombre(nombre);
        return r;
    }

    @Test
    void sinRegionGuardadaElResumenDiceGlobal() {
        usuario(1, 0);

        assertThat(service.me(1).regionCode()).isEqualTo("GLOBAL");
    }

    @Test
    void cambiarRegionGuardaElCodigoYLaLlave() {
        usuario(1, 0);
        when(regionRepo.findByCodigoAndActivaTrue("AREQUIPA")).thenReturn(Optional.of(region(4, "AREQUIPA", "Arequipa")));

        UserSummaryDto dto = service.cambiarRegion(1, "AREQUIPA");

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(guardado.capture());
        assertThat(guardado.getValue().getRegion()).isEqualTo("AREQUIPA");
        assertThat(guardado.getValue().getIdRegion()).isEqualTo(4);
        assertThat(dto.regionCode()).isEqualTo("AREQUIPA");
    }

    @Test
    void unaRegionInexistenteOInactivaDa400YNoGuarda() {
        usuario(1, 0);
        when(regionRepo.findByCodigoAndActivaTrue("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarRegion(1, "NOEXISTE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Region no valida");
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void cambiarRegionDeUnUsuarioInexistenteDa404() {
        when(usuarioRepo.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarRegion(9, "LIMA")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void lasRegionesSalenConCodigoYNombre() {
        when(regionRepo.findByActivaTrueOrderByNombreAsc())
                .thenReturn(List.of(region(1, "GLOBAL", "Global"), region(3, "LIMA", "Lima Metropolitana")));

        assertThat(service.regiones()).extracting(RegionDto::code).containsExactly("GLOBAL", "LIMA");
        assertThat(service.regiones().get(1).name()).isEqualTo("Lima Metropolitana");
    }
}
