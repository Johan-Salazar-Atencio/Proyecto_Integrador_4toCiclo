package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.plantia.dto.LogroDesbloqueadoDto;
import com.plantia.dto.LogroDto;
import com.plantia.dto.LogrosResponseDto;
import com.plantia.models.Logro;
import com.plantia.models.LogroUsuario;
import com.plantia.models.Nivel;
import com.plantia.models.ReglaProgreso;
import com.plantia.models.Usuario;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.HistorialProgresoRepository;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.LogroRepository;
import com.plantia.repositories.LogroUsuarioRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.RegistroDesastreUsuarioRepository;
import com.plantia.repositories.UsuarioRepository;

class LogroServiceTest {

    private static final int USER = 1;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 12, 0);

    private LogroRepository logroRepo;
    private LogroUsuarioRepository logroUsuarioRepo;
    private UsuarioRepository usuarioRepo;
    private EscaneoHistorialRepository escaneoRepo;
    private RegistroDesastreUsuarioRepository registroDesastreRepo;
    private MisionUsuarioRepository misionUsuarioRepo;
    private InventarioUsuarioRepository inventarioRepo;
    private ProgresoService progresoService;
    private LogroService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        logroRepo = mock(LogroRepository.class);
        logroUsuarioRepo = mock(LogroUsuarioRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        escaneoRepo = mock(EscaneoHistorialRepository.class);
        registroDesastreRepo = mock(RegistroDesastreUsuarioRepository.class);
        misionUsuarioRepo = mock(MisionUsuarioRepository.class);
        inventarioRepo = mock(InventarioUsuarioRepository.class);

        UsuarioRepository usuarioRepoProgreso = mock(UsuarioRepository.class);
        NivelRepository nivelRepo = mock(NivelRepository.class);
        ReglaProgresoRepository reglaRepo = mock(ReglaProgresoRepository.class);
        HistorialProgresoRepository historialRepo = mock(HistorialProgresoRepository.class);
        MisionService misionService = mock(MisionService.class);
        MisionUsuarioRepository misionUsuarioRepoProgreso = mock(MisionUsuarioRepository.class);
        when(nivelRepo.findAll()).thenReturn(List.of(nivel(1, 0, 0), nivel(2, 500, 20), nivel(5, 3000, 40)));
        ReglaProgreso reglaLogro = new ReglaProgreso();
        reglaLogro.setIdRegla(99);
        reglaLogro.setMotivo("LOGRO");
        reglaLogro.setActiva(true);
        when(reglaRepo.findByMotivo("LOGRO")).thenReturn(Optional.of(reglaLogro));

        progresoService = new ProgresoService(usuarioRepoProgreso, nivelRepo, reglaRepo, historialRepo, misionService, misionUsuarioRepoProgreso,
                new ObjectProvider<LogroService>() {
                    @Override public LogroService getObject(Object... args) { return service; }
                    @Override public LogroService getObject() { return service; }
                    @Override public LogroService getIfAvailable() { return service; }
                    @Override public LogroService getIfUnique() { return service; }
                    @Override public java.util.stream.Stream<LogroService> stream() { return java.util.stream.Stream.of(service); }
                    @Override public java.util.stream.Stream<LogroService> orderedStream() { return java.util.stream.Stream.of(service); }
                });
        when(usuarioRepoProgreso.findById(USER)).thenReturn(Optional.empty());

        service = new LogroService(logroRepo, logroUsuarioRepo, usuarioRepo, escaneoRepo, registroDesastreRepo, misionUsuarioRepo, inventarioRepo,
                new ObjectProvider<ProgresoService>() {
                    @Override public ProgresoService getObject(Object... args) { return progresoService; }
                    @Override public ProgresoService getObject() { return progresoService; }
                    @Override public ProgresoService getIfAvailable() { return progresoService; }
                    @Override public ProgresoService getIfUnique() { return progresoService; }
                    @Override public java.util.stream.Stream<ProgresoService> stream() { return java.util.stream.Stream.of(progresoService); }
                    @Override public java.util.stream.Stream<ProgresoService> orderedStream() { return java.util.stream.Stream.of(progresoService); }
                });

        usuario = new Usuario();
        usuario.setIdUsuario(USER);
        usuario.setNivel(1);
        usuario.setExperienciaXp(0);
        usuario.setMonedas(0);
        usuario.setRachaDias(0);
        when(usuarioRepo.findById(USER)).thenReturn(Optional.of(usuario));
        java.util.Map<Integer, LogroUsuario> estados = new java.util.HashMap<>();
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenAnswer(inv -> new java.util.ArrayList<>(estados.values()));
        when(logroUsuarioRepo.findByIdUsuarioAndIdLogro(eq(USER), anyInt())).thenAnswer(inv -> Optional.ofNullable(estados.get(inv.getArgument(1))));
        when(logroUsuarioRepo.save(any())).thenAnswer(inv -> {
            LogroUsuario lu = inv.getArgument(0);
            estados.put(lu.getIdLogro(), lu);
            return lu;
        });
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(0L);
        when(escaneoRepo.countDistinctEspeciesByIdUsuario(USER)).thenReturn(0L);
        when(registroDesastreRepo.countByUsuario(USER)).thenReturn(0L);
        when(registroDesastreRepo.countTiposDistintosByUsuario(USER)).thenReturn(0L);
        when(registroDesastreRepo.countPlantasRefugiadasByUsuario(USER)).thenReturn(0L);
        when(registroDesastreRepo.countEventosSinPerdidasByUsuario(USER)).thenReturn(0L);
        when(misionUsuarioRepo.countByIdUsuarioAndCompletadaTrue(USER)).thenReturn(0L);
        when(inventarioRepo.countComprasByUsuario(USER)).thenReturn(0L);
        when(inventarioRepo.countComprasByUsuarioYTipo(anyInt(), any())).thenReturn(0L);
    }

    private static Nivel nivel(int n, int xp, int monedas) {
        Nivel nv = new Nivel();
        nv.setNivel(n);
        nv.setXpRequerido(xp);
        nv.setRecompensaMonedas(monedas);
        return nv;
    }

    private static Logro logro(int id, String codigo, String condicion, int meta, String parametro, int xp, int monedas, boolean activo, boolean secreto) {
        Logro l = new Logro();
        l.setIdLogro(id);
        l.setCodigo(codigo);
        l.setTitulo(codigo);
        l.setDescripcion("desc");
        l.setIcono("i");
        l.setRareza("COMUN");
        l.setCondicion(condicion);
        l.setMeta(meta);
        l.setParametro(parametro);
        l.setRecompensaXp(xp);
        l.setRecompensaMonedas(monedas);
        l.setActivo(activo);
        l.setSecreto(secreto);
        l.setOrden(id);
        return l;
    }

    @Test
    void desbloqueaAlLlegarALaMetaYSumaRecompensa() {
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos).hasSize(1);
        assertThat(usuario.getExperienciaXp()).isEqualTo(20);
        assertThat(usuario.getMonedas()).isEqualTo(10);
        verify(logroUsuarioRepo, times(2)).save(any(LogroUsuario.class));
    }

    @Test
    void noDuplicaPremioAlEvaluarDosVeces() {
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);
        service.evaluar(USER, AHORA);

        LogroUsuario estado = new LogroUsuario();
        estado.setIdUsuario(USER);
        estado.setIdLogro(1);
        estado.setProgreso(1);
        estado.setDesbloqueadoEn(AHORA);
        estado.setNotificado(false);
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(estado));

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos).isEmpty();
        assertThat(usuario.getExperienciaXp()).isEqualTo(20);
    }

    @Test
    void noRevierteDesbloqueado() {
        Logro l = logro(1, "RACHA_3", "RACHA_DIAS", 3, null, 30, 10, true, false);
        LogroUsuario estado = new LogroUsuario();
        estado.setIdUsuario(USER); estado.setIdLogro(1); estado.setProgreso(3); estado.setDesbloqueadoEn(AHORA); estado.setNotificado(true);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(estado));
        usuario.setRachaDias(1);

        service.evaluar(USER, AHORA.plusDays(2));

        assertThat(estado.getDesbloqueadoEn()).isEqualTo(AHORA);
    }

    @Test
    void respetaActivo() {
        Logro l = logro(1, "X", "ESCANEOS_TOTAL", 1, null, 20, 10, false, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of());
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(5L);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);
        LogrosResponseDto listado = service.listar(USER, AHORA);

        assertThat(nuevos).isEmpty();
        assertThat(listado.logros()).isEmpty();
    }

    @Test
    void premioQueSubeDeNivelDesbloqueaNivelX() {
        Logro base = logro(1, "BASE", "ESCANEOS_TOTAL", 1, null, 500, 0, true, false);
        Logro nivel = logro(2, "NIVEL_5", "NIVEL", 5, null, 0, 0, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(base, nivel));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);
        usuario.setNivel(4);
        usuario.setExperienciaXp(0);
        // simular subida a 5 por la recompensa de nivel 2 (500 xp) no basta; usamos xp directo en logro
        base.setRecompensaXp(3000);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos.stream().map(LogroDesbloqueadoDto::codigo)).contains("BASE", "NIVEL_5");
    }

    @Test
    void desastreAfrontadoConYSinParametro() {
        Logro cualquiera = logro(1, "CUALQUIERA", "DESASTRE_AFRONTADO", 1, null, 0, 0, true, false);
        Logro huayco = logro(2, "PRIMER_HUAYCO", "DESASTRE_AFRONTADO", 1, "HUAYCO", 0, 0, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(cualquiera, huayco));
        when(registroDesastreRepo.countByUsuario(USER)).thenReturn(2L);
        when(registroDesastreRepo.countByUsuarioAndTipo(USER, "HUAYCO")).thenReturn(1L);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos).extracting(LogroDesbloqueadoDto::codigo).containsExactlyInAnyOrder("CUALQUIERA", "PRIMER_HUAYCO");
    }

    @Test
    void secretosOcultosHastaDesbloquear() {
        Logro secreto = logro(1, "MADRUGADOR", "ESCANEO_HORA", 1, "MADRUGADA", 50, 20, true, true);
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));

        LogroDto dto = service.listar(USER, AHORA).logros().get(0);
        assertThat(dto.titulo()).isEqualTo("???");
        assertThat(dto.descripcion()).isEqualTo("Logro secreto");
        assertThat(dto.icono()).isEqualTo("secreto");
        assertThat(dto.recompensaXp()).isZero();

        LogroUsuario estado = new LogroUsuario();
        estado.setIdUsuario(USER); estado.setIdLogro(1); estado.setProgreso(1); estado.setDesbloqueadoEn(AHORA); estado.setNotificado(false);
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(estado));
        LogroDto visible = service.listar(USER, AHORA).logros().get(0);
        assertThat(visible.titulo()).isEqualTo("MADRUGADOR");
        assertThat(visible.recompensaXp()).isEqualTo(50);
    }

    @Test
    void marcarVistosSoloTocaLogrosPropios() {
        LogroUsuario propio = new LogroUsuario();
        propio.setIdUsuario(USER); propio.setIdLogro(1); propio.setDesbloqueadoEn(AHORA); propio.setNotificado(false);
        when(logroUsuarioRepo.findByIdUsuarioAndIdLogro(USER, 1)).thenReturn(Optional.of(propio));
        when(logroUsuarioRepo.findByIdUsuarioAndIdLogro(USER, 99)).thenReturn(Optional.empty());

        service.marcarVistos(USER, List.of(1, 99, 100));

        assertThat(propio.getNotificado()).isTrue();
        verify(logroUsuarioRepo).save(propio);
    }
}
