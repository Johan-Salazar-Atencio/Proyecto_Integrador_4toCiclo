package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/** La mision del evento activo: prevenir el evento en todas las plantas vivas. */
class MisionEventoServiceTest {

    private static final int USER = 1;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 12, 0);

    private EventoDesastreRepository eventoRepo;
    private PlantaUsuarioRepository plantaRepo;
    private MisionEventoService service;

    @BeforeEach
    void preparar() {
        eventoRepo = mock(EventoDesastreRepository.class);
        plantaRepo = mock(PlantaUsuarioRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        Usuario u = new Usuario();
        u.setRegion("LIMA");
        when(usuarioRepo.findById(USER)).thenReturn(Optional.of(u));
        TipoDesastreRepository tipoRepo = mock(TipoDesastreRepository.class);
        TipoDesastre tipo = new TipoDesastre();
        tipo.setNombre("Ola de calor");
        when(tipoRepo.findById(3)).thenReturn(Optional.of(tipo));
        ReglaProgresoRepository reglaRepo = mock(ReglaProgresoRepository.class);
        ReglaProgreso premio = new ReglaProgreso();
        premio.setXp(80);
        premio.setMonedas(40);
        when(reglaRepo.findByMotivo("EVENTO_COMPLETO")).thenReturn(Optional.of(premio));
        service = new MisionEventoService(eventoRepo, usuarioRepo, plantaRepo, tipoRepo, reglaRepo);
    }

    private void hayEvento() {
        EventoDesastre e = new EventoDesastre();
        e.setIdDesastre(9);
        e.setTipoEvento("OLA_DE_CALOR");
        e.setIdTipoDesastre(3);
        e.setFechaFin(AHORA.plusHours(2).plusMinutes(2).plusSeconds(3));
        when(eventoRepo.activosParaRegion("LIMA")).thenReturn(List.of(e));
    }

    private static PlantaUsuario planta(boolean prevenida, String etapa) {
        PlantaUsuario p = new PlantaUsuario();
        p.setEstaRefugiada(prevenida);
        p.setEtapaCrecimiento(etapa);
        return p;
    }

    private void plantas(PlantaUsuario... ps) {
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(USER)).thenReturn(List.of(ps));
    }

    @Test
    void sinEventoActivoNoHayMision() {
        plantas(planta(false, "BROTE"));

        assertThat(service.deEvento(USER, AHORA)).isEmpty();
    }

    @Test
    void cuentaLasPlantasPrevenidasSobreLasVivas() {
        hayEvento();
        plantas(planta(true, "BROTE"), planta(false, "SEMILLA"), planta(false, "MUERTA"));

        DailyMissionDto m = service.deEvento(USER, AHORA).orElseThrow();

        assertThat(m.kind()).isEqualTo("EVENT");
        assertThat(m.eventType()).isEqualTo("OLA_DE_CALOR");
        assertThat(m.title()).isEqualTo("Prevenir ola de calor");
        assertThat(m.currentCount()).isEqualTo(1);
        assertThat(m.targetCount()).isEqualTo(2);
        assertThat(m.progress()).isEqualTo(0.5f);
        assertThat(m.isCompleted()).isFalse();
        assertThat(m.reward()).isEqualTo("+80 XP · +40 hojas");
        assertThat(m.endsInSeconds()).isEqualTo(2 * 3600 + 2 * 60 + 3);
    }

    @Test
    void estaCompletaCuandoTodasLasVivasEstanPrevenidas() {
        hayEvento();
        plantas(planta(true, "BROTE"), planta(true, "ADULTA"), planta(false, "MUERTA"));

        assertThat(service.deEvento(USER, AHORA).orElseThrow().isCompleted()).isTrue();
    }

    @Test
    void sinPlantasVivasNoHayMision() {
        hayEvento();
        plantas(planta(false, "MUERTA"));

        assertThat(service.deEvento(USER, AHORA)).isEmpty();
    }
}
