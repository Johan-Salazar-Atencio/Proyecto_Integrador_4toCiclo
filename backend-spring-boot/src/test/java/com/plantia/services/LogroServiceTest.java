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
    private NivelRepository nivelRepo;
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
        nivelRepo = mock(NivelRepository.class);
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
        verify(logroUsuarioRepo).save(org.mockito.ArgumentMatchers.argThat((LogroUsuario lu) -> lu.getDesbloqueadoEn() != null));
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
        service.marcarVistos(USER, List.of(1, 99, 100, 1));

        // Una sola sentencia, filtrada por el usuario de la sesion y sin repetidos
        verify(logroUsuarioRepo).marcarVistos(USER, new java.util.LinkedHashSet<>(List.of(1, 99, 100)));
        verify(logroUsuarioRepo, never()).findByIdUsuarioAndIdLogro(anyInt(), anyInt());
    }

    // ---------------------------------------------------------------- 1. carrera / doble pago

    @Test
    void bloqueaAlUsuarioAntesDeLeerYCreaLasFilasSinDuplicar() {
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));

        service.evaluar(USER, AHORA);

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(usuarioRepo, logroRepo, logroUsuarioRepo);
        orden.verify(usuarioRepo).bloquear(USER);
        orden.verify(usuarioRepo).findById(USER);
        orden.verify(logroRepo).findByActivoTrueOrderByOrdenAscIdLogroAsc();
        orden.verify(logroUsuarioRepo).crearFaltantes(USER); // INSERT ... ON CONFLICT DO NOTHING
    }

    @Test
    void siYaTieneTodasLasFilasNoIntentaInsertar() {
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 5, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        LogroUsuario fila = estado(1, 0, null);
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(fila));

        service.evaluar(USER, AHORA);

        verify(logroUsuarioRepo, never()).crearFaltantes(anyInt());
    }

    @Test
    void evaluarDosVecesSeguidasPagaUnaSolaVez() {
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);

        List<LogroDesbloqueadoDto> primera = service.evaluar(USER, AHORA);
        List<LogroDesbloqueadoDto> segunda = service.evaluar(USER, AHORA.plusMinutes(1));

        assertThat(primera).hasSize(1);
        assertThat(segunda).isEmpty();
        assertThat(usuario.getExperienciaXp()).isEqualTo(20);
        assertThat(usuario.getMonedas()).isEqualTo(10);
        verify(usuarioRepo, org.mockito.Mockito.atLeast(2)).bloquear(USER); // cada evaluacion bloquea primero
    }

    // ---------------------------------------------------------------- 2. cuidados con contador propio

    @Test
    void riegosAbonosYLuzUsanLosContadoresDelUsuarioNoElHistorial() {
        Logro riegos = logro(1, "MANO_VERDE", "RIEGOS_TOTAL", 50, null, 100, 40, true, false);
        Logro abonos = logro(2, "BUEN_ABONO", "ABONOS_TOTAL", 1, null, 15, 5, true, false);
        Logro luz = logro(3, "SOL_MANANA", "LUZ_TOTAL", 2, null, 15, 5, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(riegos, abonos, luz));
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(riegos, abonos, luz));
        usuario.setRiegosTotal(50); // muchos mas que los 5 por dia que deja la regla REGAR en el historial
        usuario.setAbonosTotal(1);
        usuario.setLuzTotal(1);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos).extracting(LogroDesbloqueadoDto::codigo).containsExactly("MANO_VERDE", "BUEN_ABONO");
        assertThat(service.listar(USER, AHORA).logros()).filteredOn(d -> d.codigo().equals("SOL_MANANA"))
                .singleElement().satisfies(d -> assertThat(d.progreso()).isEqualTo(1));
    }

    // ---------------------------------------------------------------- 3. un fallo de logros no tumba la accion

    @Test
    void evaluarLanzaParaQueSuTransaccionSeDeshagaEntera() {
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc())
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("base caida"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.evaluar(USER, AHORA))
                .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
    }

    @Test
    void evaluarSeguroNuncaLanza() {
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc())
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("base caida"));

        assertThat(service.evaluarSeguro(USER, AHORA)).isEmpty();
    }

    @Test
    void conTransaccionAbiertaEvaluaRecienDespuesDelCommitYLlenaLaMismaLista() {
        TxFalsa tx = new TxFalsa();
        service = conTransacciones(tx);
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);

        List<List<LogroDesbloqueadoDto>> listas = new java.util.ArrayList<>();
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(s -> {
            listas.add(service.evaluarSeguro(USER, AHORA));
            listas.add(service.evaluarSeguro(USER, AHORA)); // misma transaccion: una sola evaluacion
            // Dentro de la accion principal todavia no se evaluo nada
            assertThat(listas.get(0)).isEmpty();
            verify(usuarioRepo, never()).bloquear(USER);
        });

        assertThat(listas.get(0)).extracting(LogroDesbloqueadoDto::codigo).containsExactly("PRIMERA_PLANTA");
        assertThat(listas.get(1)).isSameAs(listas.get(0));
        verify(usuarioRepo, org.mockito.Mockito.atLeastOnce()).bloquear(USER);
        assertThat(tx.commits).isEqualTo(2); // la accion y, aparte, la de logros
        assertThat(usuario.getExperienciaXp()).isEqualTo(20);
    }

    @Test
    void siLosLogrosFallanTrasElCommitLaAccionPrincipalYaQuedoGuardadaYNoHayExcepcion() {
        TxFalsa tx = new TxFalsa();
        service = conTransacciones(tx);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc())
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("base caida"));

        List<List<LogroDesbloqueadoDto>> listas = new java.util.ArrayList<>();
        new org.springframework.transaction.support.TransactionTemplate(tx)
                .executeWithoutResult(s -> listas.add(service.evaluarSeguro(USER, AHORA)));

        assertThat(listas.get(0)).isEmpty();
        assertThat(tx.commits).isEqualTo(1);   // la accion principal
        assertThat(tx.rollbacks).isEqualTo(1); // solo la transaccion de logros
    }

    @Test
    void siFallaElPremioNoQuedaElLogroDesbloqueadoAMedias() {
        TxFalsa tx = new TxFalsa();
        service = conTransacciones(tx);
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);
        when(nivelRepo.findAll()).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("base caida"));

        List<LogroDesbloqueadoDto> nuevos = service.evaluarSeguro(USER, AHORA);

        assertThat(nuevos).isEmpty();
        assertThat(tx.rollbacks).isEqualTo(1); // desbloqueo + premio van juntos: se deshacen juntos
        assertThat(tx.commits).isZero();
    }

    // ---------------------------------------------------------------- 4. secretos

    @Test
    void secretoBloqueadoNoFiltraSuCodigoNiNadaSuyo() throws Exception {
        Logro secreto = logro(7, "MADRUGADOR", "ESCANEO_HORA", 1, "MADRUGADA", 50, 20, true, true);
        secreto.setTitulo("Madrugador");
        secreto.setDescripcion("Escanea una planta antes de las 6:00.");
        secreto.setIcono("amanecer");
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));

        LogrosResponseDto listado = service.listar(USER, AHORA);

        LogroDto dto = listado.logros().get(0);
        assertThat(dto.codigo()).isEqualTo("secreto_7");
        assertThat(dto.titulo()).isEqualTo("???");
        assertThat(dto.descripcion()).isEqualTo("Logro secreto");
        assertThat(dto.icono()).isEqualTo("secreto");
        String json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(listado);
        assertThat(json).doesNotContain("MADRUGADOR", "Madrugador", "6:00", "amanecer", "MADRUGADA");
    }

    @Test
    void secretoDesbloqueadoMuestraSuCodigoReal() {
        Logro secreto = logro(7, "MADRUGADOR", "ESCANEO_HORA", 1, "MADRUGADA", 50, 20, true, true);
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(estado(7, 1, AHORA)));

        assertThat(service.listar(USER, AHORA).logros().get(0).codigo()).isEqualTo("MADRUGADOR");
    }

    // ---------------------------------------------------------------- 5. zona horaria

    @Test
    void escaneoHoraSeCalculaEnLaZonaDeLimaPorDefecto() {
        Logro madrugador = logro(1, "MADRUGADOR", "ESCANEO_HORA", 1, "MADRUGADA", 50, 20, true, true);
        Logro nocturno = logro(2, "NOCTURNO", "ESCANEO_HORA", 1, "NOCHE", 50, 20, true, true);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(madrugador, nocturno));
        String servidor = LogroService.zonaParaPostgres(java.time.ZoneId.systemDefault());
        when(escaneoRepo.countEscaneosEnFranja(USER, 0, 5, servidor, "America/Lima")).thenReturn(1L);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos).extracting(LogroDesbloqueadoDto::codigo).containsExactly("MADRUGADOR");
        verify(escaneoRepo, org.mockito.Mockito.atLeastOnce()).countEscaneosEnFranja(USER, 23, 23, servidor, "America/Lima");
    }

    @Test
    void laZonaDelJugadorSeConfiguraConLaPropiedad() {
        LogroService enMadrid = new LogroService(logroRepo, logroUsuarioRepo, usuarioRepo, escaneoRepo, registroDesastreRepo,
                misionUsuarioRepo, inventarioRepo, proveedor(progresoService), null, "Europe/Madrid");
        Logro madrugador = logro(1, "MADRUGADOR", "ESCANEO_HORA", 1, "MADRUGADA", 50, 20, true, true);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(madrugador));

        enMadrid.evaluar(USER, AHORA);

        verify(escaneoRepo).countEscaneosEnFranja(eq(USER), eq(0), eq(5), any(), eq("Europe/Madrid"));
        assertThat(new LogroService(logroRepo, logroUsuarioRepo, usuarioRepo, escaneoRepo, registroDesastreRepo,
                misionUsuarioRepo, inventarioRepo, proveedor(progresoService), null, " ").zonaJugador().getId())
                .isEqualTo("America/Lima");
    }

    @Test
    void zonaConDesfaseFijoSeEscribeEnFormatoPosix() {
        assertThat(LogroService.zonaParaPostgres(java.time.ZoneId.of("America/Lima"))).isEqualTo("America/Lima");
        assertThat(LogroService.zonaParaPostgres(java.time.ZoneOffset.ofHours(-5))).isEqualTo("<-05:00>+05:00");
        assertThat(LogroService.zonaParaPostgres(java.time.ZoneOffset.ofHoursMinutes(5, 30))).isEqualTo("<+05:30>-05:30");
        assertThat(LogroService.zonaParaPostgres(java.time.ZoneOffset.UTC)).isEqualTo("UTC");
    }

    // ---------------------------------------------------------------- casos de meta y activo

    @Test
    void desbloqueadoNoSeRevierteSiBajaElContadorNiSiElAdminSubeLaMeta() {
        Logro l = logro(1, "ESCANEOS_10", "ESCANEOS_TOTAL", 10, null, 50, 20, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(10L);
        service.evaluar(USER, AHORA);

        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(0L); // ya no cuenta esos escaneos
        l.setMeta(30); // el admin sube la meta
        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA.plusDays(1));
        LogroDto dto = service.listar(USER, AHORA.plusDays(1)).logros().get(0);

        assertThat(nuevos).isEmpty();
        assertThat(dto.desbloqueado()).isTrue();
        assertThat(dto.desbloqueadoEn()).isEqualTo(AHORA);
        assertThat(dto.progreso()).isEqualTo(dto.meta());
        assertThat(usuario.getExperienciaXp()).isEqualTo(50);
    }

    @Test
    void siElAdminBajaLaMetaSeDesbloqueaUnaSolaVezEnLaSiguienteEvaluacion() {
        Logro l = logro(1, "ESCANEOS_10", "ESCANEOS_TOTAL", 10, null, 50, 20, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(6L);
        assertThat(service.evaluar(USER, AHORA)).isEmpty();

        l.setMeta(5);
        assertThat(service.evaluar(USER, AHORA)).hasSize(1);
        assertThat(service.evaluar(USER, AHORA)).isEmpty();
        assertThat(usuario.getExperienciaXp()).isEqualTo(50);
    }

    @Test
    void logroNuevoYaCumplidoSeDesbloqueaUnaSolaVez() {
        Logro viejo = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 0, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(viejo));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(12L);
        service.evaluar(USER, AHORA);

        Logro nuevo = logro(2, "ESCANEOS_10", "ESCANEOS_TOTAL", 10, null, 50, 0, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(viejo, nuevo));
        List<LogroDesbloqueadoDto> segunda = service.evaluar(USER, AHORA);
        List<LogroDesbloqueadoDto> tercera = service.evaluar(USER, AHORA);

        assertThat(segunda).extracting(LogroDesbloqueadoDto::codigo).containsExactly("ESCANEOS_10");
        assertThat(tercera).isEmpty();
        assertThat(usuario.getExperienciaXp()).isEqualTo(70);
    }

    @Test
    void desactivadoNoSeEvaluaPeroElYaGanadoSigueVisible() {
        Logro ganado = logro(1, "VIEJO", "ESCANEOS_TOTAL", 1, null, 20, 0, false, false);
        Logro pendiente = logro(2, "OTRO", "COMPRAS_TIENDA", 1, null, 20, 0, false, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of());
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(ganado, pendiente));
        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(estado(1, 1, AHORA)));
        when(inventarioRepo.countComprasByUsuario(USER)).thenReturn(3L);

        LogrosResponseDto listado = service.listar(USER, AHORA);

        assertThat(listado.logros()).extracting(LogroDto::codigo).containsExactly("VIEJO");
        assertThat(listado.desbloqueados()).isEqualTo(1);
        verify(inventarioRepo, never()).countComprasByUsuario(anyInt());
    }

    // ---------------------------------------------------------------- racha

    @Test
    void rachaUsaLaRachaActualSiSigueVigente() {
        Logro l = logro(1, "RACHA_3", "RACHA_DIAS", 3, null, 30, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        usuario.setRachaDias(3);
        usuario.setUltimaActividad(AHORA.toLocalDate().minusDays(1)); // ayer: hoy aun puede seguirla

        assertThat(service.evaluar(USER, AHORA)).hasSize(1);
    }

    @Test
    void rachaRotaNoDesbloqueaAunqueRachaDiasNoSeHayaReiniciado() {
        Logro l = logro(1, "RACHA_3", "RACHA_DIAS", 3, null, 30, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        usuario.setRachaDias(5);
        usuario.setUltimaActividad(AHORA.toLocalDate().minusDays(4)); // dejo de jugar: la racha ya se perdio

        assertThat(service.evaluar(USER, AHORA)).isEmpty();
        assertThat(LogroService.rachaVigente(usuario, AHORA)).isZero();
    }

    // ---------------------------------------------------------------- marcarVistos

    @Test
    void marcarVistosNoTocaFilasDeOtroUsuario() {
        service.marcarVistos(USER, java.util.Arrays.asList(5, null));
        service.marcarVistos(null, List.of(5));
        service.marcarVistos(USER, java.util.Arrays.asList((Integer) null));

        verify(logroUsuarioRepo).marcarVistos(USER, java.util.Set.of(5));
        verify(logroUsuarioRepo, times(1)).marcarVistos(anyInt(), any());
    }

    @Test
    void marcarVistosRechazaMasDeCienIds() {
        List<Integer> muchos = java.util.stream.IntStream.rangeClosed(1, LogroService.MAX_VISTOS + 1).boxed().toList();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.marcarVistos(USER, muchos))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verify(logroUsuarioRepo, never()).marcarVistos(anyInt(), any());

        service.marcarVistos(USER, muchos.subList(0, LogroService.MAX_VISTOS)); // 100 si se aceptan
        verify(logroUsuarioRepo).marcarVistos(eq(USER), any());
    }

    // ---------------------------------------------------------------- cadena de niveles

    @Test
    void cadenaDeNivelesTieneTopeYLoQueFaltaSeDesbloqueaDespuesSinRepetir() {
        when(nivelRepo.findAll()).thenReturn(List.of(nivel(1, 0, 0), nivel(2, 100, 0), nivel(3, 200, 0),
                nivel(4, 300, 0), nivel(5, 400, 0), nivel(6, 500, 0)));
        // Orden al reves: cada premio sube un nivel y solo la evaluacion anidada ve el siguiente logro
        Logro n6 = logro(1, "NIVEL_6", "NIVEL", 6, null, 100, 0, true, false);
        Logro n5 = logro(2, "NIVEL_5", "NIVEL", 5, null, 100, 0, true, false);
        Logro n4 = logro(3, "NIVEL_4", "NIVEL", 4, null, 100, 0, true, false);
        Logro n3 = logro(4, "NIVEL_3", "NIVEL", 3, null, 100, 0, true, false);
        Logro n2 = logro(5, "NIVEL_2", "NIVEL", 2, null, 100, 0, true, false);
        Logro base = logro(6, "BASE", "ESCANEOS_TOTAL", 1, null, 100, 0, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(n6, n5, n4, n3, n2, base));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);

        List<LogroDesbloqueadoDto> primera = service.evaluar(USER, AHORA);
        List<LogroDesbloqueadoDto> segunda = service.evaluar(USER, AHORA);
        List<LogroDesbloqueadoDto> tercera = service.evaluar(USER, AHORA);

        assertThat(primera).extracting(LogroDesbloqueadoDto::codigo).containsExactly("BASE", "NIVEL_2", "NIVEL_3");
        assertThat(segunda).extracting(LogroDesbloqueadoDto::codigo).containsExactly("NIVEL_4", "NIVEL_5", "NIVEL_6");
        assertThat(tercera).isEmpty();
        assertThat(usuario.getExperienciaXp()).isEqualTo(600); // 6 logros x 100, cada uno una vez
        assertThat(usuario.getNivel()).isEqualTo(6);
    }

    // ---------------------------------------------------------------- utilidades

    private static LogroUsuario estado(int idLogro, int progreso, LocalDateTime desbloqueadoEn) {
        LogroUsuario lu = new LogroUsuario();
        lu.setIdLogroUsuario(100 + idLogro);
        lu.setIdUsuario(USER);
        lu.setIdLogro(idLogro);
        lu.setProgreso(progreso);
        lu.setDesbloqueadoEn(desbloqueadoEn);
        lu.setNotificado(false);
        return lu;
    }

    private LogroService conTransacciones(org.springframework.transaction.PlatformTransactionManager tx) {
        return new LogroService(logroRepo, logroUsuarioRepo, usuarioRepo, escaneoRepo, registroDesastreRepo,
                misionUsuarioRepo, inventarioRepo, proveedor(progresoService), tx, "America/Lima");
    }

    private static <T> ObjectProvider<T> proveedor(T valor) {
        return new ObjectProvider<T>() {
            @Override public T getObject(Object... args) { return valor; }
            @Override public T getObject() { return valor; }
            @Override public T getIfAvailable() { return valor; }
            @Override public T getIfUnique() { return valor; }
            @Override public java.util.stream.Stream<T> stream() { return java.util.stream.Stream.of(valor); }
            @Override public java.util.stream.Stream<T> orderedStream() { return java.util.stream.Stream.of(valor); }
        };
    }

    /** Gestor de transacciones de mentira: corre de verdad las sincronizaciones (afterCommit) de Spring. */
    static class TxFalsa extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;

        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object t, org.springframework.transaction.TransactionDefinition d) { }
        @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus s) { commits++; }
        @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus s) { rollbacks++; }
    }

    // ---------------------------------------------------------------- B1: sin segunda conexion en la peticion

    @Test
    void enUnaPeticionWebElCommitNoAbreOtraTransaccionYSeEvaluaAlSoltarLaConexion() {
        TxFalsa tx = new TxFalsa();
        service = conTransacciones(tx);
        Logro l = logro(1, "PRIMERA_PLANTA", "ESCANEOS_TOTAL", 1, null, 20, 10, true, false);
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(l));
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(1L);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
        try {
            List<List<LogroDesbloqueadoDto>> listas = new java.util.ArrayList<>();
            org.springframework.transaction.support.TransactionTemplate accion = new org.springframework.transaction.support.TransactionTemplate(tx);
            accion.executeWithoutResult(s -> listas.add(service.evaluarSeguro(USER, AHORA)));
            accion.executeWithoutResult(s -> listas.add(service.evaluarSeguro(USER, AHORA))); // otra accion, misma peticion

            // Tras los commits: ninguna transaccion de logros abierta mientras la accion tenia su conexion
            assertThat(tx.commits).isEqualTo(2);
            assertThat(listas.get(0)).isEmpty();
            verify(usuarioRepo, never()).bloquear(USER);

            // Dentro de otra transaccion tampoco: esperaria a soltarla
            accion.executeWithoutResult(s -> service.evaluarPendientesDeLaPeticion());
            verify(usuarioRepo, never()).bloquear(USER);

            service.evaluarPendientesDeLaPeticion(); // lo que hacen LogrosTrasLaAccion y LogrosPendientesFilter
            service.evaluarPendientesDeLaPeticion(); // la segunda vez ya no hay nada anotado

            assertThat(listas.get(0)).extracting(LogroDesbloqueadoDto::codigo).containsExactly("PRIMERA_PLANTA");
            assertThat(listas.get(1)).extracting(LogroDesbloqueadoDto::codigo).containsExactly("PRIMERA_PLANTA");
            assertThat(tx.commits).isEqualTo(4); // 3 acciones + 1 sola evaluacion para el usuario
            assertThat(usuario.getExperienciaXp()).isEqualTo(20);
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void siLaAccionSeRevierteNoSeAnotaNada() {
        TxFalsa tx = new TxFalsa();
        service = conTransacciones(tx);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
        try {
            org.springframework.transaction.support.TransactionTemplate accion = new org.springframework.transaction.support.TransactionTemplate(tx);
            accion.executeWithoutResult(s -> {
                service.evaluarSeguro(USER, AHORA);
                s.setRollbackOnly();
            });
            service.evaluarPendientesDeLaPeticion();

            verify(usuarioRepo, never()).bloquear(USER);
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    // ---------------------------------------------------------------- consultas casi constantes

    @Test
    void unaConsultaPorCondicionAunqueHayaMuchosLogrosYVariasVueltas() {
        List<Logro> muchos = new java.util.ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            muchos.add(logro(i, "ESC_" + i, "ESCANEOS_TOTAL", i, null, 0, 0, true, false));
        }
        muchos.add(logro(201, "COMPRA", "COMPRAS_TIENDA", 1, null, 3000, 0, true, false)); // sube de nivel
        muchos.add(logro(202, "NIVEL_5", "NIVEL", 5, null, 0, 0, true, false));
        java.util.Collections.reverse(muchos); // NIVEL_5 primero: necesita una segunda vuelta
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(muchos);
        when(escaneoRepo.countByIdUsuarioAndIdCatalogoIsNotNull(USER)).thenReturn(50L);
        when(inventarioRepo.countComprasByUsuario(USER)).thenReturn(1L);

        List<LogroDesbloqueadoDto> nuevos = service.evaluar(USER, AHORA);

        assertThat(nuevos).hasSize(50 + 2);
        verify(escaneoRepo, times(1)).countByIdUsuarioAndIdCatalogoIsNotNull(USER);
        verify(inventarioRepo, times(1)).countComprasByUsuario(USER);
        verify(nivelRepo, times(1)).findAll(); // datos del premio: una vez, no una por logro
    }

    // ---------------------------------------------------------------- secretos: rareza

    @Test
    void secretoBloqueadoNoMuestraSuRareza() {
        Logro secreto = logro(7, "MADRUGADOR", "ESCANEO_HORA", 1, "MADRUGADA", 50, 20, true, true);
        secreto.setRareza("LEGENDARIA");
        when(logroRepo.findAllByOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));
        when(logroRepo.findByActivoTrueOrderByOrdenAscIdLogroAsc()).thenReturn(List.of(secreto));

        assertThat(service.listar(USER, AHORA).logros().get(0).rareza()).isEqualTo("COMUN");

        when(logroUsuarioRepo.findByIdUsuario(USER)).thenReturn(List.of(estado(7, 1, AHORA)));
        assertThat(service.listar(USER, AHORA).logros().get(0).rareza()).isEqualTo("LEGENDARIA");
    }
}
