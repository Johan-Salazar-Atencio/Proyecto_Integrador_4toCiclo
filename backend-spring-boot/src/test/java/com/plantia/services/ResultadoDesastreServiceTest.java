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
import com.plantia.models.ReglaProgreso;
import com.plantia.models.TipoDesastre;
import com.plantia.models.Usuario;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.HistorialProgresoRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.RegistroDesastreUsuarioRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.TipoDesastreRepository;
import com.plantia.repositories.UsuarioRepository;

/** Resultado por planta al terminar un evento: SALVADA (prevenido) o DANADA (casi muerta, con plazo para abonar). */
class ResultadoDesastreServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 12, 0);

    private EventoDesastreRepository eventoRepo;
    private PlantaUsuarioRepository plantaRepo;
    private RegistroDesastreUsuarioRepository registroRepo;
    private TipoDesastreRepository tipoRepo;
    private Usuario usuario;
    private ResultadoDesastreService service;

    @BeforeEach
    void preparar() {
        eventoRepo = mock(EventoDesastreRepository.class);
        plantaRepo = mock(PlantaUsuarioRepository.class);
        registroRepo = mock(RegistroDesastreUsuarioRepository.class);
        tipoRepo = mock(TipoDesastreRepository.class);

        // Progreso real con repositorios simulados: la regla REFUGIO da 40 de XP
        usuario = new Usuario();
        usuario.setIdUsuario(5);
        usuario.setNivel(1);
        usuario.setExperienciaXp(0);
        usuario.setMonedas(0);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        when(usuarioRepo.findById(5)).thenReturn(Optional.of(usuario));
        ReglaProgreso refugio = new ReglaProgreso();
        refugio.setIdRegla(1);
        refugio.setMotivo("REFUGIO");
        refugio.setXp(40);
        ReglaProgreso completo = new ReglaProgreso();
        completo.setIdRegla(2);
        completo.setMotivo("EVENTO_COMPLETO");
        completo.setXp(80);
        completo.setMonedas(40);
        ReglaProgresoRepository reglaRepo = mock(ReglaProgresoRepository.class);
        when(reglaRepo.findByMotivo("REFUGIO")).thenReturn(Optional.of(refugio));
        when(reglaRepo.findByMotivo("EVENTO_COMPLETO")).thenReturn(Optional.of(completo));
        ProgresoService progreso = new ProgresoService(usuarioRepo, mock(NivelRepository.class), reglaRepo,
                mock(HistorialProgresoRepository.class), mock(MisionService.class),
                mock(MisionUsuarioRepository.class));

        service = new ResultadoDesastreService(eventoRepo, plantaRepo, registroRepo, tipoRepo, progreso,
                new ParametrosJuego(mock(com.plantia.repositories.ParametroJuegoRepository.class)), 6);
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
        p.setIdUsuario(5);
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
    void salvarUnaPlantaDaXpPeroNoCuentaComoActividadDelDia() {
        PlantaUsuario p = planta(7, 80, true);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        // 40 por la planta prevenida y 80 por completar el evento (era la unica planta)
        assertThat(registroGuardado().getXpOtorgado()).isEqualTo(40);
        assertThat(usuario.getExperienciaXp()).isEqualTo(120);
        assertThat(usuario.getMonedas()).isEqualTo(40);
        assertThat(usuario.getUltimaActividad()).isNull();
    }

    @Test
    void unaPlantaDanadaNoDaXp() {
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        assertThat(registroGuardado().getXpOtorgado()).isZero();
        assertThat(usuario.getExperienciaXp()).isZero();
    }

    @Test
    void plantaSinPrevenirQuedaCasiMuertaSegunSeveridadYConPlazoParaAbonar() {
        PlantaUsuario p = planta(7, 100, false);
        EventoDesastre e = evento("CRITICO", AHORA.minusMinutes(1));
        e.setTipoEvento("OLA_DE_CALOR");
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(e, AHORA);

        RegistroDesastreUsuario r = registroGuardado();
        assertThat(r.getEstadoFinal()).isEqualTo("DANADA");
        assertThat(r.getDanoSaludAplicado()).isEqualTo(90);
        assertThat(p.getSaludPorcentaje()).isEqualTo(10);
        assertThat(p.getRecuperacionHasta()).isEqualTo(AHORA.plusHours(24));
        assertThat(p.getGolpeTipo()).isEqualTo("OLA_DE_CALOR");
    }

    @Test
    void laSeveridadBajaDejaMasSaludQueLaCritica() {
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("BAJO", AHORA.minusMinutes(1)), AHORA);

        assertThat(p.getSaludPorcentaje()).isEqualTo(40);
    }

    @Test
    void unaPlantaQueYaTeniaPocaSaludNoSube() {
        PlantaUsuario p = planta(7, 5, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        RegistroDesastreUsuario r = registroGuardado();
        assertThat(r.getDanoSaludAplicado()).isZero();
        assertThat(p.getSaludPorcentaje()).isEqualTo(5);
        assertThat(p.getRecuperacionHasta()).isNotNull();
    }

    @Test
    void usaLaSaludFinalDelTipoDeDesastreCuandoExiste() {
        TipoDesastre tipo = new TipoDesastre();
        tipo.setSaludFinalModerado(35);
        EventoDesastre e = evento("MODERADO", AHORA.minusMinutes(1));
        e.setIdTipoDesastre(3);
        when(tipoRepo.findById(3)).thenReturn(Optional.of(tipo));
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(e, AHORA);

        assertThat(p.getSaludPorcentaje()).isEqualTo(35);
    }

    @Test
    void prevenirEnTodasLasPlantasCompletaElEventoYEnUnaSolaNo() {
        PlantaUsuario prevenida = planta(7, 80, true);
        PlantaUsuario golpeada = planta(8, 80, false);
        when(plantaRepo.findAll()).thenReturn(List.of(prevenida, golpeada));

        service.procesar(evento("MODERADO", AHORA.minusMinutes(1)), AHORA);

        // Solo el premio por la planta prevenida (40); sin el de evento completo
        assertThat(usuario.getExperienciaXp()).isEqualTo(40);
        assertThat(usuario.getMonedas()).isZero();
    }

    @Test
    void procesarUnDesastreNoReiniciaElRelojDeDesgasteDeLaPlanta() {
        LocalDateTime antes = AHORA.minusHours(5);
        PlantaUsuario p = planta(7, 100, false);
        p.setUpdatedAt(antes);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("BAJO", AHORA.minusMinutes(1)), AHORA);

        assertThat(p.getUpdatedAt()).isEqualTo(antes);
    }

    @Test
    void eventoDeUnaRegionSoloAfectaAPlantasDeEsaRegion() {
        EventoDesastre e = evento("BAJO", AHORA.minusMinutes(1));
        e.setRegion("Arequipa");
        PlantaUsuario p = planta(7, 100, false);
        when(plantaRepo.findDeUsuariosDeRegion("Arequipa")).thenReturn(List.of(p));

        service.procesar(e, AHORA);

        verify(plantaRepo, never()).findAll();
        assertThat(p.getSaludPorcentaje()).isEqualTo(40);
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

    @Test
    void lasPlantasDelBaulNoSufrenElEvento() {
        PlantaUsuario p = planta(7, 100, false);
        p.setEnBaul(true);
        when(plantaRepo.findAll()).thenReturn(List.of(p));

        service.procesar(evento("CRITICO", AHORA.minusMinutes(1)), AHORA);

        verify(registroRepo, never()).save(any());
        assertThat(p.getSaludPorcentaje()).isEqualTo(100);
        assertThat(p.getRecuperacionHasta()).isNull();
    }
}
