package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.plantia.models.EventoDesastre;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.HistorialProgresoRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.RegistroDesastreUsuarioRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.TipoDesastreRepository;
import com.plantia.repositories.UsuarioRepository;

/** PROGRAMADO -> ACTIVO -> FINALIZADO segun las fechas, y resultado al finalizar. */
class EventoCicloServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 12, 0);

    private EventoDesastreRepository eventoRepo;
    private PlantaUsuarioRepository plantaRepo;
    private EventoCicloService ciclo;

    @BeforeEach
    void preparar() {
        eventoRepo = mock(EventoDesastreRepository.class);
        plantaRepo = mock(PlantaUsuarioRepository.class);
        ProgresoService progreso = new ProgresoService(mock(UsuarioRepository.class), mock(NivelRepository.class),
                mock(ReglaProgresoRepository.class), mock(HistorialProgresoRepository.class),
                mock(MisionService.class), mock(MisionUsuarioRepository.class));
        ResultadoDesastreService resultados = new ResultadoDesastreService(
                eventoRepo, plantaRepo, mock(RegistroDesastreUsuarioRepository.class), mock(TipoDesastreRepository.class), progreso,
                new ParametrosJuego(mock(com.plantia.repositories.ParametroJuegoRepository.class)), 6);
        ciclo = new EventoCicloService(eventoRepo, resultados);
    }

    private static EventoDesastre evento(String estado, LocalDateTime inicio, LocalDateTime fin) {
        EventoDesastre e = new EventoDesastre();
        e.setIdDesastre(1);
        e.setRegion("GLOBAL");
        e.setEstado(estado);
        e.setFechaInicio(inicio);
        e.setFechaFin(fin);
        return e;
    }

    @Test
    void programadoSeActivaCuandoLlegaSuFechaDeInicio() {
        EventoDesastre e = evento("PROGRAMADO", AHORA.minusMinutes(1), AHORA.plusHours(2));
        when(eventoRepo.findByEstadoAndFechaInicioLessThanEqual("PROGRAMADO", AHORA)).thenReturn(List.of(e));

        ciclo.avanzar(AHORA);

        assertThat(e.getEstado()).isEqualTo("ACTIVO");
        verify(eventoRepo).save(e);
    }

    @Test
    void programadoConLaVentanaYaPasadaVaDirectoAFinalizado() {
        EventoDesastre e = evento("PROGRAMADO", AHORA.minusHours(5), AHORA.minusHours(1));
        when(eventoRepo.findByEstadoAndFechaInicioLessThanEqual("PROGRAMADO", AHORA)).thenReturn(List.of(e));

        ciclo.avanzar(AHORA);

        assertThat(e.getEstado()).isEqualTo("FINALIZADO");
    }

    @Test
    void activoVencidoSeFinalizaYSeDevuelve() {
        EventoDesastre e = evento("ACTIVO", AHORA.minusHours(3), AHORA.minusSeconds(5));
        when(eventoRepo.findByEstadoAndFechaFinLessThanEqual("ACTIVO", AHORA)).thenReturn(List.of(e));

        List<EventoDesastre> finalizados = ciclo.avanzar(AHORA);

        assertThat(finalizados).containsExactly(e);
        assertThat(e.getEstado()).isEqualTo("FINALIZADO");
    }

    @Test
    void alFinalizarSeProcesanLosEventosPendientes() {
        EventoDesastre e = evento("FINALIZADO", AHORA.minusHours(3), AHORA.minusSeconds(5));
        when(eventoRepo.findByEstadoAndProcesado("FINALIZADO", false)).thenReturn(List.of(e));
        when(plantaRepo.findAll()).thenReturn(List.of());

        ciclo.avanzar(AHORA);

        assertThat(e.getProcesado()).isTrue();
    }

    @Test
    void sinEventosNoSeGuardaNada() {
        ciclo.avanzar(AHORA);

        verify(eventoRepo, org.mockito.Mockito.never()).save(any());
    }
}
