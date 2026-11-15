package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.plantia.dto.DailyMissionDto;
import com.plantia.models.Mision;
import com.plantia.models.MisionUsuario;
import com.plantia.repositories.MisionRepository;
import com.plantia.repositories.MisionUsuarioRepository;

/** Misiones del dia: las activas las decide el admin y cada usuario lleva su propio avance. */
class MisionServiceTest {

    private static final int USER = 1;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    private MisionRepository misionRepo;
    private MisionUsuarioRepository misionUsuarioRepo;
    private MisionService service;

    @BeforeEach
    void preparar() {
        misionRepo = mock(MisionRepository.class);
        misionUsuarioRepo = mock(MisionUsuarioRepository.class);
        service = new MisionService(misionRepo, misionUsuarioRepo);
        when(misionUsuarioRepo.save(any(MisionUsuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Mision mision(int id, String titulo, String tipo, int meta, int xp, int monedas) {
        Mision m = new Mision();
        m.setIdMision(id);
        m.setTitulo(titulo);
        m.setTipo(tipo);
        m.setMeta(meta);
        m.setRecompensaXp(xp);
        m.setRecompensaMonedas(monedas);
        m.setActiva(true);
        return m;
    }

    private static MisionUsuario avance(int idMision, int progreso, boolean completada) {
        MisionUsuario mu = new MisionUsuario();
        mu.setIdUsuario(USER);
        mu.setIdMision(idMision);
        mu.setProgreso(progreso);
        mu.setCompletada(completada);
        mu.setFecha(HOY);
        return mu;
    }

    @Test
    void creaElAvanceDeHoyParaCadaMisionActivaQueAunNoLoTiene() {
        when(misionRepo.findByActivaTrueOrderByOrdenAscIdMisionAsc())
                .thenReturn(List.of(mision(1, "Riego", "RIEGO", 2, 20, 50), mision(2, "Escaneo", "ESCANEO", 1, 20, 30)));
        when(misionUsuarioRepo.findByIdUsuarioAndFecha(USER, HOY)).thenReturn(List.of(avance(1, 1, false)));

        List<MisionService.Asignada> hoy = service.asignadas(USER, HOY);

        assertThat(hoy).hasSize(2);
        assertThat(hoy.get(0).estado().getProgreso()).isEqualTo(1); // el que ya existia no se reinicia
        assertThat(hoy.get(1).estado().getProgreso()).isZero();
        assertThat(hoy.get(1).estado().getFecha()).isEqualTo(HOY);
        verify(misionUsuarioRepo).save(hoy.get(1).estado()); // solo se crea el que faltaba
    }

    @Test
    void sinMisionesActivasLaListaVaciaYNoHayDiaria() {
        when(misionRepo.findByActivaTrueOrderByOrdenAscIdMisionAsc()).thenReturn(List.of());

        assertThat(service.asignadas(USER, HOY)).isEmpty();
        assertThat(service.diaria(USER)).isEmpty();
        verify(misionUsuarioRepo, never()).save(any());
    }

    @Test
    void elDtoMuestraAvanceMetaYRecompensaDeLaMision() {
        MisionService.Asignada a = new MisionService.Asignada(mision(1, "Riego", "RIEGO", 4, 20, 50), avance(1, 1, false));

        DailyMissionDto dto = MisionService.aDto(a);

        assertThat(dto.id()).isEqualTo("mission_1");
        assertThat(dto.progress()).isEqualTo(0.25f);
        assertThat(dto.currentCount()).isEqualTo(1);
        assertThat(dto.targetCount()).isEqualTo(4);
        assertThat(dto.reward()).contains("20 XP").contains("50");
        assertThat(dto.isCompleted()).isFalse();
    }

    @Test
    void unaMetaInvalidaNoDividePorCero() {
        Mision m = mision(1, "Rara", "RIEGO", 0, 0, 0);
        MisionService.Asignada a = new MisionService.Asignada(m, avance(1, 3, false));

        assertThat(MisionService.aDto(a).targetCount()).isEqualTo(1);
        assertThat(MisionService.aDto(a).progress()).isEqualTo(1f);
    }

    @Test
    void laDiariaDelHomeEsLaPrimeraPendienteOLaPrimeraSiEstanTodasCompletas() {
        when(misionRepo.findByActivaTrueOrderByOrdenAscIdMisionAsc())
                .thenReturn(List.of(mision(1, "Riego", "RIEGO", 2, 20, 50), mision(2, "Escaneo", "ESCANEO", 1, 20, 30)));
        when(misionUsuarioRepo.findByIdUsuarioAndFecha(any(), any()))
                .thenReturn(List.of(avance(1, 2, true), avance(2, 0, false)));

        assertThat(service.diaria(USER)).get().extracting(DailyMissionDto::id).isEqualTo("mission_2");

        when(misionUsuarioRepo.findByIdUsuarioAndFecha(any(), any()))
                .thenReturn(List.of(avance(1, 2, true), avance(2, 1, true)));

        assertThat(service.diaria(USER)).get().extracting(DailyMissionDto::id).isEqualTo("mission_1");
    }
}
