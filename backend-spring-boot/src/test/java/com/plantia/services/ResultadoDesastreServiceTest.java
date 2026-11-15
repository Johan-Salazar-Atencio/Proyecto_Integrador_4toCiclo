package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.plantia.models.EventoDesastre;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.RegistroDesastreUsuario;
import com.plantia.models.TipoDesastre;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.RegistroDesastreUsuarioRepository;
import com.plantia.repositories.TipoDesastreRepository;

/** Resultado por planta al terminar un evento: SALVADA, DANADA o DESTRUIDA. */
class ResultadoDesastreServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 12, 0);

    private EventoDesastreRepository eventoRepo;
    private PlantaUsuarioRepository plantaRepo;
    private RegistroDesastreUsuarioRepository registroRepo;
    private TipoDesastreRepository tipoRepo;
    private ResultadoDesastreService service;

    @BeforeEach
    void preparar() {
        eventoRepo = mock(EventoDesastreRepository.class);
        plantaRepo = mock(PlantaUsuarioRepository.class);
        registroRepo = mock(RegistroDesastreUsuarioRepository.class);
        tipoRepo = mock(TipoDesastreRepository.class);
        service = new ResultadoDesastreService(eventoRepo, plantaRepo, registroRepo, tipoRepo, 6);
    }

    private static EventoDesastre evento(String severidad, LocalDateTime fin) {
        EventoDesastre e = new EventoDesastre();
        e.setIdDesastre(1);
        e.setRegion("GLOBAL");
        e.setSeveridad(severidad);
        e.setFechaFin(fin);
        e.setEstado("FINALIZADO");
        return e;
    }

    private static PlantaUsuario planta(int id, int salud, boolean refugiada) {
        PlantaUsuario p = new PlantaUsuario();
        p.setIdPlantaUsuario(id);
        p.setSaludPorcentaje(salud);
        p.setEstaRefugiada(refugiada);
        p.setEtapaCrecimiento("BROTE");
        return p;
    }

    private RegistroDesastreUsuario registroGuardado() {
        ArgumentCaptor<RegistroDesastreUsuario> captor = ArgumentCaptor.forClass(RegistroDesastreUsuario.class);
        verify(registroRepo).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void plantaRefugiadaQuedaSalvadaSinDanoYSeLimpiaElRefugio() {
        PlantaUsuario p = planta(7, 80, true);
        p.setRefugiadaDesde(AHORA.minusHours(1));
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        RegistroDesastreUsuario r = registroGuardado();
        assertThat(r.getEstadoFinal()).isEqualTo("SALVADA");
        assertThat(r.getEstabaRefugiada()).isTrue();
        assertThat(r.getDanoSaludAplicado()).isZero();
        assertThat(p.getSaludPorcentaje()).isEqualTo(80);
        assertThat(p.getEstaRefugiada()).isFalse();
        assertThat(p.getRefugiadaDesde()).isNull();
    }

    @Test
    void plantaSinRefugioPierdeSaludSegunSeveridad() {
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        RegistroDesastreUsuario r = registroGuardado();
        assertThat(r.getEstadoFinal()).isEqualTo("DANADA");
        assertThat(r.getDanoSaludAplicado()).isEqualTo(50);
        assertThat(p.getSaludPorcentaje()).isEqualTo(50);
    }

    @Test
    void saludQueLlegaACeroQuedaDestruidaYElDanoEsLoQueTenia() {
        PlantaUsuario p = planta(7, 20, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        RegistroDesastreUsuario r = registroGuardado();
        assertThat(r.getEstadoFinal()).isEqualTo("DESTRUIDA");
        assertThat(r.getDanoSaludAplicado()).isEqualTo(20);
        assertThat(p.getSaludPorcentaje()).isZero();
    }

    @Test
    void usaElDanoDelTipoDeDesastreCuandoExiste() {
        TipoDesastre tipo = new TipoDesastre();
        tipo.setDanoModerado(40);
        EventoDesastre e = evento("MODERADO", AHORA.minusMinutes(1));
        e.setIdTipoDesastre(3);
        when(tipoRepo.findById(3)).thenReturn(Optional.of(tipo));
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(e, AHORA);

        assertThat(p.getSaludPorcentaje()).isEqualTo(60);
    }

    @Test
    void eventoDeUnaRegionSoloAfectaAPlantasDeEsaRegion() {
        EventoDesastre e = evento("BAJO", AHORA.minusMinutes(1));
        e.setRegion("Arequipa");
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findDeUsuariosDeRegion("Arequipa")).thenReturn(List.of(p));

        service.procesar(e, AHORA);

        verify(plantaRepo, never()).findAll();
        assertThat(p.getSaludPorcentaje()).isEqualTo(90);
    }

    @Test
    void eventoTerminadoHaceMasQueLaGraciaSeMarcaProcesadoSinDanar() {
        EventoDesastre e = evento("CRITICO", AHORA.minusDays(26));
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(e, AHORA);

        assertThat(e.getProcesado()).isTrue();
        assertThat(p.getSaludPorcentaje()).isEqualTo(100);
        verify(registroRepo, never()).save(any());
    }

    @Test
    void noRegistraDosVecesLaMismaPlantaEnUnEvento() {
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));
        when(registroRepo.existsByIdDesastreAndIdPlantaUsuario(1, 7)).thenReturn(true);

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        verify(registroRepo, never()).save(any());
        assertThat(p.getSaludPorcentaje()).isEqualTo(100);
    }

    @Test
    void plantasMuertasSeIgnoran() {
        PlantaUsuario p = planta(7, 0, false);
        p.setEtapaCrecimiento("MUERTA");
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        verify(registroRepo, never()).save(any());
    }

    @Test
    void despuesDeProcesarElEventoQuedaMarcadoYGuardado() {
        EventoDesastre e = evento("BAJO", AHORA.minusMinutes(1));
        when(plantaRepo.findAll()).thenReturn(List.of());

        service.procesar(e, AHORA);

        assertThat(e.getProcesado()).isTrue();
        verify(eventoRepo).save(e);
    }
}
