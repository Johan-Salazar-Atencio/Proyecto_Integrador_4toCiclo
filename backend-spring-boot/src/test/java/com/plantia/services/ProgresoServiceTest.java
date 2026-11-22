package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.ProgresoDto;
import com.plantia.models.HistorialProgreso;
import com.plantia.models.Mision;
import com.plantia.models.MisionUsuario;
import com.plantia.models.Nivel;
import com.plantia.models.ReglaProgreso;
import com.plantia.models.Usuario;
import com.plantia.repositories.HistorialProgresoRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.UsuarioRepository;

/** XP, nivel, monedas y racha: reglas de reglas_progreso y tabla niveles. */
class ProgresoServiceTest {

    private static final int USER = 1;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 12, 0);

    private UsuarioRepository usuarioRepo;
    private ReglaProgresoRepository reglaRepo;
    private HistorialProgresoRepository historialRepo;
    private MisionService misionService;
    private MisionUsuarioRepository misionUsuarioRepo;
    private ProgresoService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioRepo = mock(UsuarioRepository.class);
        reglaRepo = mock(ReglaProgresoRepository.class);
        historialRepo = mock(HistorialProgresoRepository.class);
        NivelRepository nivelRepo = mock(NivelRepository.class);
        // Mismos valores que la tabla niveles de la base (hasta el 4 alcanza para las pruebas)
        when(nivelRepo.findAll()).thenReturn(List.of(
                nivel(1, 0, 0), nivel(2, 500, 20), nivel(3, 1000, 25), nivel(4, 1700, 30)));
        misionService = mock(MisionService.class);
        misionUsuarioRepo = mock(MisionUsuarioRepository.class);
        service = new ProgresoService(usuarioRepo, nivelRepo, reglaRepo, historialRepo, misionService, misionUsuarioRepo);

        usuario = new Usuario();
        usuario.setIdUsuario(USER);
        usuario.setNivel(1);
        usuario.setExperienciaXp(0);
        usuario.setMonedas(100);
        usuario.setRachaDias(0);
        usuario.setUltimaActividad(AHORA.toLocalDate()); // hoy: la racha no interviene salvo que la prueba lo pida
        when(usuarioRepo.findById(USER)).thenReturn(Optional.of(usuario));
    }

    private static Nivel nivel(int n, int xp, int monedas) {
        Nivel nv = new Nivel();
        nv.setNivel(n);
        nv.setXpRequerido(xp);
        nv.setRecompensaMonedas(monedas);
        return nv;
    }

    private void regla(int id, String motivo, int xp, int monedas, Integer tope) {
        ReglaProgreso r = new ReglaProgreso();
        r.setIdRegla(id);
        r.setMotivo(motivo);
        r.setXp(xp);
        r.setMonedas(monedas);
        r.setTopeDiario(tope);
        when(reglaRepo.findByMotivo(motivo)).thenReturn(Optional.of(r));
    }

    @Test
    void sumaXpYRegistraElMovimiento() {
        regla(1, "ESCANEO", 20, 0, null);

        ProgresoDto p = service.otorgar(USER, List.of("ESCANEO"), 77, AHORA);

        assertThat(p.xpGanado()).isEqualTo(20);
        assertThat(p.experienciaXp()).isEqualTo(20);
        assertThat(p.subioDeNivel()).isFalse();
        ArgumentCaptor<HistorialProgreso> h = ArgumentCaptor.forClass(HistorialProgreso.class);
        verify(historialRepo).save(h.capture());
        assertThat(h.getValue().getIdUsuario()).isEqualTo(USER);
        assertThat(h.getValue().getIdRegla()).isEqualTo(1);
        assertThat(h.getValue().getXpGanado()).isEqualTo(20);
        assertThat(h.getValue().getNivelResultante()).isEqualTo(1);
        assertThat(h.getValue().getIdReferencia()).isEqualTo(77);
        verify(usuarioRepo).save(usuario);
    }

    @Test
    void alLlegarAlXpDelSiguienteNivelSubeYCobraLaRecompensa() {
        usuario.setExperienciaXp(480);
        regla(1, "ESCANEO", 20, 5, null);

        ProgresoDto p = service.otorgar(USER, List.of("ESCANEO"), null, AHORA);

        assertThat(p.nivel()).isEqualTo(2);
        assertThat(p.subioDeNivel()).isTrue();
        assertThat(p.monedasGanadas()).isEqualTo(25); // 5 de la regla + 20 del nivel 2
        assertThat(usuario.getMonedas()).isEqualTo(125);
        assertThat(usuario.getNivel()).isEqualTo(2);
    }

    @Test
    void puedeSubirVariosNivelesConUnaSolaGanancia() {
        regla(1, "BONO", 1100, 0, null);

        ProgresoDto p = service.otorgar(USER, List.of("BONO"), null, AHORA);

        assertThat(p.nivel()).isEqualTo(3);
        assertThat(p.monedasGanadas()).isEqualTo(45); // 20 + 25
    }

    @Test
    void enElUltimoNivelSigueSumandoXpSinSubir() {
        usuario.setNivel(4);
        usuario.setExperienciaXp(1700);
        regla(1, "ESCANEO", 20, 0, null);

        ProgresoDto p = service.otorgar(USER, List.of("ESCANEO"), null, AHORA);

        assertThat(p.nivel()).isEqualTo(4);
        assertThat(p.subioDeNivel()).isFalse();
        assertThat(p.experienciaXp()).isEqualTo(1720);
    }

    @Test
    void unaAccionPuedeAplicarVariasReglas() {
        regla(1, "ESCANEO", 20, 0, null);
        regla(2, "ESPECIE_NUEVA", 50, 0, null);

        ProgresoDto p = service.otorgar(USER, List.of("ESCANEO", "ESPECIE_NUEVA"), null, AHORA);

        assertThat(p.xpGanado()).isEqualTo(70);
        verify(historialRepo, times(2)).save(any());
    }

    @Test
    void conElTopeDiarioAlcanzadoNoSumaNiRegistra() {
        regla(1, "CUIDAR", 5, 0, 10);
        when(historialRepo.countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(
                eq(USER), eq(1), eq(AHORA.toLocalDate().atStartOfDay()))).thenReturn(10L);

        ProgresoDto p = service.otorgar(USER, List.of("CUIDAR"), null, AHORA);

        assertThat(p.xpGanado()).isZero();
        assertThat(usuario.getExperienciaXp()).isZero();
        verify(historialRepo, never()).save(any());
    }

    @Test
    void debajoDelTopeDiarioSiSuma() {
        regla(1, "CUIDAR", 5, 0, 10);
        when(historialRepo.countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(eq(USER), eq(1), any())).thenReturn(9L);

        assertThat(service.otorgar(USER, List.of("CUIDAR"), null, AHORA).xpGanado()).isEqualTo(5);
    }

    @Test
    void reglaInexistenteOInactivaNoSumaYNoFalla() {
        ReglaProgreso inactiva = new ReglaProgreso();
        inactiva.setIdRegla(3);
        inactiva.setMotivo("REFUGIO");
        inactiva.setXp(40);
        inactiva.setActiva(false);
        when(reglaRepo.findByMotivo("REFUGIO")).thenReturn(Optional.of(inactiva));

        ProgresoDto p = service.otorgar(USER, List.of("INEXISTENTE", "REFUGIO"), null, AHORA);

        assertThat(p.xpGanado()).isZero();
        verify(historialRepo, never()).save(any());
    }

    @Test
    void usuarioInexistenteDa404() {
        assertThatThrownBy(() -> service.otorgar(99, List.of("ESCANEO"), null, AHORA))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void primeraActividadDelDiaMantieneLaRachaQueYaTeniaYDaElBono() {
        usuario.setUltimaActividad(null);
        usuario.setRachaDias(3);
        regla(9, "RACHA", 10, 0, 1);

        ProgresoDto p = service.otorgar(USER, List.of(), null, AHORA);

        assertThat(p.rachaDias()).isEqualTo(3);
        assertThat(p.xpGanado()).isEqualTo(10);
        assertThat(usuario.getUltimaActividad()).isEqualTo(AHORA.toLocalDate());
    }

    @Test
    void ayerActivoSumaUnDiaALaRacha() {
        usuario.setUltimaActividad(AHORA.toLocalDate().minusDays(1));
        usuario.setRachaDias(4);

        assertThat(service.otorgar(USER, List.of(), null, AHORA).rachaDias()).isEqualTo(5);
    }

    @Test
    void siSeSaltoUnDiaLaRachaVuelveAUno() {
        usuario.setUltimaActividad(AHORA.toLocalDate().minusDays(2));
        usuario.setRachaDias(8);

        assertThat(service.otorgar(USER, List.of(), null, AHORA).rachaDias()).isEqualTo(1);
    }

    @Test
    void conActividadYaRegistradaHoyNoCambiaLaRachaNiDaOtroBono() {
        usuario.setUltimaActividad(LocalDate.of(2026, 11, 15));
        usuario.setRachaDias(6);
        regla(9, "RACHA", 10, 0, 1);

        ProgresoDto p = service.otorgar(USER, List.of(), null, AHORA);

        assertThat(p.rachaDias()).isEqualTo(6);
        assertThat(p.xpGanado()).isZero();
        verify(historialRepo, never()).save(any());
    }

    @Test
    void sinReglasCargadasLaAccionNoFallaYLaRachaSeActualiza() {
        usuario.setUltimaActividad(null);
        usuario.setRachaDias(0);
        when(reglaRepo.findByMotivo(any())).thenReturn(Optional.empty());

        ProgresoDto p = service.otorgar(USER, List.of("ESCANEO"), null, AHORA);

        assertThat(p.rachaDias()).isEqualTo(1);
        assertThat(p.xpGanado()).isZero();
        verify(historialRepo, never()).countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(anyInt(), anyInt(), any());
    }

    @Test
    void elPremioSinRachaSumaXpPeroNoTocaLaRachaNiLaActividad() {
        usuario.setUltimaActividad(AHORA.toLocalDate().minusDays(1));
        usuario.setRachaDias(4);
        regla(3, "REFUGIO", 40, 0, null);
        regla(9, "RACHA", 10, 0, 1);

        ProgresoDto p = service.otorgarSinRacha(USER, "REFUGIO", 12, AHORA);

        assertThat(p.xpGanado()).isEqualTo(40); // sin el bono de racha
        assertThat(usuario.getRachaDias()).isEqualTo(4);
        assertThat(usuario.getUltimaActividad()).isEqualTo(AHORA.toLocalDate().minusDays(1));
    }

    // --- Misiones del admin: el avance y la recompensa salen de la tabla misiones ---

    private MisionService.Asignada mision(int id, String tipo, int meta, int xp, int monedas, int progreso, boolean completada) {
        Mision m = new Mision();
        m.setIdMision(id);
        m.setTipo(tipo);
        m.setMeta(meta);
        m.setRecompensaXp(xp);
        m.setRecompensaMonedas(monedas);
        MisionUsuario mu = new MisionUsuario();
        mu.setIdUsuario(USER);
        mu.setIdMision(id);
        mu.setProgreso(progreso);
        mu.setCompletada(completada);
        return new MisionService.Asignada(m, mu);
    }

    private void misionesDeHoy(MisionService.Asignada... asignadas) {
        when(misionService.asignadas(eq(USER), any())).thenReturn(List.of(asignadas));
    }

    @Test
    void unaAccionAvanzaLaMisionDeSuTipoSinPagarAunqueNoLlegueALaMeta() {
        MisionService.Asignada riego = mision(1, "RIEGO", 2, 20, 50, 0, false);
        misionesDeHoy(riego);

        ProgresoDto p = service.registrarAccion(USER, "RIEGO", 5, AHORA);

        assertThat(riego.estado().getProgreso()).isEqualTo(1);
        assertThat(riego.estado().getCompletada()).isFalse();
        assertThat(p.xpGanado()).isZero();
        verify(misionUsuarioRepo).save(riego.estado());
    }

    @Test
    void alLlegarALaMetaCompletaLaMisionYPagaXpYMonedasDeLaMision() {
        regla(5, "MISION", 0, 0, null);
        MisionService.Asignada riego = mision(1, "RIEGO", 2, 20, 50, 1, false);
        misionesDeHoy(riego);

        ProgresoDto p = service.registrarAccion(USER, "RIEGO", 5, AHORA);

        assertThat(riego.estado().getCompletada()).isTrue();
        assertThat(p.xpGanado()).isEqualTo(20);
        assertThat(p.monedasGanadas()).isEqualTo(50);
        assertThat(usuario.getMonedas()).isEqualTo(150);
        ArgumentCaptor<HistorialProgreso> h = ArgumentCaptor.forClass(HistorialProgreso.class);
        verify(historialRepo).save(h.capture());
        assertThat(h.getValue().getIdRegla()).isEqualTo(5);
        assertThat(h.getValue().getIdReferencia()).isEqualTo(1); // la mision
    }

    @Test
    void unaMisionYaCompletadaNoVuelveAPagar() {
        MisionService.Asignada riego = mision(1, "RIEGO", 2, 20, 50, 2, true);
        misionesDeHoy(riego);

        ProgresoDto p = service.registrarAccion(USER, "RIEGO", 5, AHORA);

        assertThat(p.xpGanado()).isZero();
        assertThat(riego.estado().getProgreso()).isEqualTo(2);
        verify(misionUsuarioRepo, never()).save(any());
    }

    @Test
    void unaAccionNoTocaMisionesDeOtroTipo() {
        MisionService.Asignada escaneo = mision(2, "ESCANEO", 1, 20, 30, 0, false);
        misionesDeHoy(escaneo);

        service.registrarAccion(USER, "RIEGO", 5, AHORA);

        assertThat(escaneo.estado().getProgreso()).isZero();
        verify(misionUsuarioRepo, never()).save(any());
    }

    @Test
    void variasMisionesDelMismoTipoAvanzanALaVez() {
        MisionService.Asignada a = mision(1, "REFUGIO", 1, 40, 0, 0, false);
        MisionService.Asignada b = mision(7, "REFUGIO", 3, 10, 0, 0, false);
        misionesDeHoy(a, b);

        ProgresoDto p = service.registrarAccion(USER, "REFUGIO", 9, AHORA);

        assertThat(a.estado().getCompletada()).isTrue();
        assertThat(b.estado().getProgreso()).isEqualTo(1);
        assertThat(p.xpGanado()).isEqualTo(40);
    }

    @Test
    void registrarAccionNoTocaLaRachaNiDaElBono() {
        usuario.setUltimaActividad(AHORA.toLocalDate().minusDays(1));
        usuario.setRachaDias(4);
        regla(9, "RACHA", 10, 0, 1);
        misionesDeHoy(mision(3, "REFUGIO", 1, 40, 0, 0, false));

        ProgresoDto p = service.registrarAccion(USER, "REFUGIO", 9, AHORA);

        assertThat(p.xpGanado()).isEqualTo(40);
        assertThat(usuario.getRachaDias()).isEqualTo(4);
    }

    @Test
    void sinLaReglaMisionLaRecompensaIgualSeCobraSinHistorial() {
        misionesDeHoy(mision(1, "RIEGO", 1, 20, 0, 0, false));

        ProgresoDto p = service.registrarAccion(USER, "RIEGO", 5, AHORA);

        assertThat(p.xpGanado()).isEqualTo(20);
        verify(historialRepo, never()).save(any());
    }

    @Test
    void registrarCuidadoBloqueaAlUsuarioYSumaElContadorCorrecto() {
        service.registrarCuidado(USER, "REGAR");
        service.registrarCuidado(USER, "SOL");
        service.registrarCuidado(USER, "ABONAR");
        service.registrarCuidado(USER, "OTRA");

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(usuarioRepo);
        orden.verify(usuarioRepo).bloquear(USER);
        orden.verify(usuarioRepo).findById(USER);
        assertThat(usuario.getRiegosTotal()).isEqualTo(1);
        assertThat(usuario.getLuzTotal()).isEqualTo(1);
        assertThat(usuario.getAbonosTotal()).isEqualTo(1);
    }

    @Test
    void elRiegoCuentaParaLogrosAunqueLaReglaYaLlegoASuTopeDiario() {
        regla(3, "REGAR", 2, 1, 5);
        when(historialRepo.countByIdUsuarioAndIdReglaAndFechaGreaterThanEqual(eq(USER), eq(3), any())).thenReturn(5L);

        for (int i = 0; i < 8; i++) {
            service.registrarCuidado(USER, "REGAR");
            assertThat(service.otorgar(USER, List.of("REGAR"), 10, AHORA).xpGanado()).isZero();
        }

        assertThat(usuario.getRiegosTotal()).isEqualTo(8);
        verify(historialRepo, never()).save(any());
    }
}
