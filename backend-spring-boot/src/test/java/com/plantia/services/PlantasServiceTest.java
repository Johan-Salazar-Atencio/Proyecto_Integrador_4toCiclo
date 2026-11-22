package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantia.models.EscaneoHistorial;
import com.plantia.models.EventoDesastre;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.Usuario;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.HistorialProgresoRepository;
import com.plantia.repositories.MisionUsuarioRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.ParametroJuegoRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.ReglaProgresoRepository;
import com.plantia.repositories.UsuarioRepository;

/** Un usuario no puede tocar plantas ni escaneos de otro (antes bastaba con cambiar el id de la URL). */
class PlantasServiceTest {

    private static final int DUENO = 1;
    private static final int OTRO = 2;

    private PlantaUsuarioRepository plantaRepo;
    private EscaneoHistorialRepository escaneoRepo;
    private EventoDesastreRepository eventoRepo;
    private PlantasService service;
    private UsuarioRepository usuarioRepo;
    private PlantaCatalogoRepository catalogoRepo;
    private com.plantia.repositories.ItemTiendaRepository itemRepo;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        plantaRepo = mock(PlantaUsuarioRepository.class);
        escaneoRepo = mock(EscaneoHistorialRepository.class);
        // Sin reglas cargadas: el progreso no suma nada, pero el flujo de cuidar y digitalizar se ejecuta completo
        usuarioRepo = mock(UsuarioRepository.class);
        ProgresoService progreso = new ProgresoService(usuarioRepo, mock(NivelRepository.class),
                mock(ReglaProgresoRepository.class), mock(HistorialProgresoRepository.class),
                mock(MisionService.class), mock(MisionUsuarioRepository.class));
        eventoRepo = mock(EventoDesastreRepository.class);
        EventoService eventos = new EventoService(eventoRepo, usuarioRepo, mock(com.plantia.repositories.TipoDesastreRepository.class));
        catalogoRepo = mock(PlantaCatalogoRepository.class);
        itemRepo = mock(com.plantia.repositories.ItemTiendaRepository.class);
        com.plantia.models.PlantaCatalogo especie = new com.plantia.models.PlantaCatalogo();
        especie.setIdCatalogo(5);
        when(catalogoRepo.findById(5)).thenReturn(Optional.of(especie));
        usuario = new Usuario();
        when(usuarioRepo.findById(any())).thenReturn(Optional.of(usuario));
        service = new PlantasService(plantaRepo, catalogoRepo, escaneoRepo, progreso, eventos,
                usuarioRepo, new ParametrosJuego(mock(ParametroJuegoRepository.class)), itemRepo, new ObjectMapper());

        PlantaUsuario planta = new PlantaUsuario();
        planta.setIdUsuario(DUENO);
        planta.setHidratacionPorcentaje(50);
        planta.setSolPorcentaje(50);
        planta.setSaludPorcentaje(50);
        planta.setEtapaCrecimiento("SEMILLA");
        when(plantaRepo.findById(10)).thenReturn(Optional.of(planta));
        when(plantaRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        EscaneoHistorial escaneo = new EscaneoHistorial();
        escaneo.setIdUsuario(DUENO);
        escaneo.setIdCatalogo(5);
        when(escaneoRepo.findById(20)).thenReturn(Optional.of(escaneo));
    }

    @Test
    void cuidarPlantaAjenaDa404YNoGuarda() {
        assertThatThrownBy(() -> service.cuidar(OTRO, 10, "REGAR"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void activarPlantaAjenaDa404() {
        assertThatThrownBy(() -> service.activar(OTRO, 10))
                .isInstanceOf(ResponseStatusException.class);
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void crearDesdeEscaneoAjenoDa404() {
        assertThatThrownBy(() -> service.digitalizar(OTRO, 20, null, null))
                .isInstanceOf(ResponseStatusException.class);
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void elDuenoSiPuedeRegar() {
        assertThat(service.cuidar(DUENO, 10, "REGAR")).isNotNull();
        verify(plantaRepo).save(any());
    }

    @Test
    void sinCuidadosSeSecaPorHorasCompletas() {
        PlantaUsuario p = new PlantaUsuario();
        p.setHidratacionPorcentaje(60);
        p.setSolPorcentaje(70);
        p.setSaludPorcentaje(90);
        var antes = java.time.LocalDateTime.of(2026, 1, 1, 10, 0);
        p.setUpdatedAt(antes);

        // 59 minutos: nada cambia (y no se pierde ese tiempo)
        PlantasService.envejecer(p, antes.plusMinutes(59));
        assertThat(p.getHidratacionPorcentaje()).isEqualTo(60);
        assertThat(p.getUpdatedAt()).isEqualTo(antes);

        // 10 horas: agua -40 (queda en 20, critica) y luz -30, asi que la salud baja 2 por hora
        PlantasService.envejecer(p, antes.plusHours(10).plusMinutes(30));
        assertThat(p.getHidratacionPorcentaje()).isEqualTo(20);
        assertThat(p.getSolPorcentaje()).isEqualTo(40);
        assertThat(p.getSaludPorcentaje()).isEqualTo(70);
        assertThat(p.getUpdatedAt()).isEqualTo(antes.plusHours(10));
    }

    @Test
    void variosCuidadosEnUnDiaCuentanUnSoloDia() {
        for (int i = 0; i < 2; i++) {
            service.cuidar(DUENO, 10, "ABONAR");
        }
        var dto = service.cuidar(DUENO, 10, "SOL");
        var planta = plantaRepo.findById(10).get();
        assertThat(dto.crecimiento()).isEqualTo(3);
        assertThat(planta.getDiasCuidados()).isEqualTo(1);
        assertThat(planta.getEtapaCrecimiento()).isEqualTo("SEMILLA");
    }

    @Test
    void cadaCuidadoTieneSuReglaYAvanzaLaMisionDeCuidadoCompleto() {
        assertThat(PlantasService.motivoDeCuidado("REGAR")).isEqualTo("REGAR");
        assertThat(PlantasService.motivoDeCuidado("SOL")).isEqualTo("SOL");
        assertThat(PlantasService.motivoDeCuidado("ABONAR")).isEqualTo("ABONAR");
        assertThat(PlantasService.accionesDeCuidado("REGAR")).containsExactly("RIEGO", "CUIDADO");
        assertThat(PlantasService.accionesDeCuidado("SOL")).containsExactly("SOL", "CUIDADO");
        assertThat(PlantasService.accionesDeCuidado("ABONAR")).containsExactly("ABONO", "CUIDADO");
    }

    @Test
    void laDuracionDependeDeLaForma() {
        assertThat(CrecimientoPlanta.diasTotales("cactus")).isEqualTo(5);
        assertThat(CrecimientoPlanta.diasTotales("monstera")).isEqualTo(7);
        assertThat(CrecimientoPlanta.diasTotales("arbol")).isEqualTo(12);
        assertThat(CrecimientoPlanta.diasTotales((String) null)).isEqualTo(7);
        assertThat(CrecimientoPlanta.diasTotales("rara")).isEqualTo(7);
    }

    @Test
    void lasFasesEmpiezanAl35PorCientoYTerminanAl100() {
        // 5 dias: crecimiento desde el dia 2; 7 dias: desde el 3; 12 dias: desde el 5
        assertThat(CrecimientoPlanta.faseSegun(1, 5)).isEqualTo("SEMILLA");
        assertThat(CrecimientoPlanta.faseSegun(2, 5)).isEqualTo("BROTE");
        assertThat(CrecimientoPlanta.faseSegun(5, 5)).isEqualTo("ADULTA");
        assertThat(CrecimientoPlanta.faseSegun(2, 7)).isEqualTo("SEMILLA");
        assertThat(CrecimientoPlanta.faseSegun(3, 7)).isEqualTo("BROTE");
        assertThat(CrecimientoPlanta.faseSegun(4, 12)).isEqualTo("SEMILLA");
        assertThat(CrecimientoPlanta.faseSegun(5, 12)).isEqualTo("BROTE");
        assertThat(CrecimientoPlanta.faseSegun(12, 12)).isEqualTo("ADULTA");
    }

    @Test
    void saltarseUnDiaPausaLaPlantaSinRetroceder() {
        var p = new PlantaUsuario();
        p.setEtapaCrecimiento("SEMILLA");
        var lunes = java.time.LocalDate.of(2026, 11, 16);
        PlantasService.contarDia(p, lunes, 7);
        PlantasService.contarDia(p, lunes, 7); // mismo dia: no suma
        PlantasService.contarDia(p, lunes.plusDays(3), 7); // se salto dos dias: suma solo uno
        assertThat(p.getDiasCuidados()).isEqualTo(2);
        assertThat(p.getEtapaCrecimiento()).isEqualTo("SEMILLA");
        PlantasService.contarDia(p, lunes.plusDays(4), 7);
        assertThat(p.getDiasCuidados()).isEqualTo(3);
        assertThat(p.getEtapaCrecimiento()).isEqualTo("BROTE");
    }

    @Test
    void losDiasNoPasanDelTotalYLaFaseNoRetrocede() {
        var p = new PlantaUsuario();
        p.setEtapaCrecimiento("ADULTA");
        p.setDiasCuidados(5);
        PlantasService.contarDia(p, java.time.LocalDate.of(2026, 11, 16), 5);
        assertThat(p.getDiasCuidados()).isEqualTo(5);
        // Planta antigua ya ADULTA con pocos dias: conserva la fase
        var vieja = new PlantaUsuario();
        vieja.setEtapaCrecimiento("ADULTA");
        vieja.setDiasCuidados(4);
        PlantasService.contarDia(vieja, java.time.LocalDate.of(2026, 11, 16), 12);
        assertThat(vieja.getEtapaCrecimiento()).isEqualTo("ADULTA");
    }

    // --- Prevenir el evento (antes refugio) ---

    private void hayDesastreActivo() {
        when(eventoRepo.activosParaRegion("GLOBAL")).thenReturn(List.of(new EventoDesastre()));
    }

    @Test
    void prevenirConUnEventoActivoMarcaLaPlantaYNoDaXp() {
        hayDesastreActivo();

        var dto = service.prevenir(DUENO, 10);

        assertThat(dto.refugiada()).isTrue();
        assertThat(dto.progreso()).isNull();
        verify(plantaRepo).save(any());
    }

    @Test
    void sinDesastreActivoDa409YNoGuarda() {
        assertThatThrownBy(() -> service.prevenir(DUENO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void prevenirUnaPlantaYaProtegidaEsInofensivoAunSinDesastre() {
        hayDesastreActivo();
        service.prevenir(DUENO, 10);
        when(eventoRepo.activosParaRegion("GLOBAL")).thenReturn(List.of());

        var dto = service.prevenir(DUENO, 10);

        assertThat(dto.refugiada()).isTrue();
        verify(plantaRepo, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void prevenirPlantaAjenaDa404() {
        hayDesastreActivo();

        assertThatThrownBy(() -> service.prevenir(OTRO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void unaPlantaMuertaNoSePuedePrevenir() {
        hayDesastreActivo();
        plantaRepo.findById(10).get().setEtapaCrecimiento("MUERTA");

        assertThatThrownBy(() -> service.prevenir(DUENO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void prevenirNoReiniciaElRelojDeDesgaste() {
        hayDesastreActivo();
        var planta = plantaRepo.findById(10).get();
        var antes = java.time.LocalDateTime.now().minusHours(3);
        planta.setUpdatedAt(antes);

        service.prevenir(DUENO, 10);

        assertThat(planta.getUpdatedAt()).isEqualTo(antes);
        assertThat(planta.getRefugiadaDesde()).isNotNull();
    }

    @Test
    void elNombreAnteriorSigueFuncionando() {
        hayDesastreActivo();

        assertThat(service.refugiar(DUENO, 10).refugiada()).isTrue();
    }

    // --- Morir sin abono y revivir ---

    private static PlantaUsuario golpeada(LocalDateTime limite) {
        PlantaUsuario p = new PlantaUsuario();
        p.setHidratacionPorcentaje(80);
        p.setSolPorcentaje(80);
        p.setSaludPorcentaje(10);
        p.setEtapaCrecimiento("BROTE");
        p.setDiasCuidados(3);
        p.setRecuperacionHasta(limite);
        p.setUpdatedAt(limite.minusHours(24));
        return p;
    }

    @Test
    void unaPlantaGolpeadaMuereCuandoPasaElPlazoSinAbonar() {
        LocalDateTime limite = LocalDateTime.of(2026, 11, 16, 12, 0);
        PlantaUsuario p = golpeada(limite);

        PlantasService.envejecer(p, limite.plusMinutes(1));

        assertThat(p.getEtapaCrecimiento()).isEqualTo("MUERTA");
        assertThat(p.getSaludPorcentaje()).isZero();
        assertThat(p.getRecuperacionHasta()).isNull();
    }

    @Test
    void dentroDelPlazoSigueViva() {
        LocalDateTime limite = LocalDateTime.of(2026, 11, 16, 12, 0);
        PlantaUsuario p = golpeada(limite);

        PlantasService.envejecer(p, limite.minusHours(2));

        assertThat(p.getEtapaCrecimiento()).isEqualTo("BROTE");
        assertThat(p.getRecuperacionHasta()).isEqualTo(limite);
    }

    @Test
    void unaPlantaMuertaNoSeDesgastaMas() {
        PlantaUsuario p = golpeada(LocalDateTime.of(2026, 11, 16, 12, 0));
        p.setEtapaCrecimiento("MUERTA");
        p.setRecuperacionHasta(null);
        p.setSaludPorcentaje(0);

        PlantasService.envejecer(p, LocalDateTime.of(2026, 11, 20, 12, 0));

        assertThat(p.getHidratacionPorcentaje()).isEqualTo(80);
    }

    private PlantaUsuario muerta() {
        PlantaUsuario p = plantaRepo.findById(10).get();
        p.setEtapaCrecimiento("MUERTA");
        p.setSaludPorcentaje(0);
        p.setDiasCuidados(3);
        return p;
    }

    @Test
    void revivirCuestaHojasYDevuelveLaPlantaConMediaSalud() {
        PlantaUsuario p = muerta();
        usuario.setMonedas(300);

        var dto = service.revivir(DUENO, 10);

        assertThat(usuario.getMonedas()).isEqualTo(50);
        assertThat(dto.healthPercent()).isEqualTo(50);
        assertThat(dto.fase()).isNotEqualTo("MUERTA");
        assertThat(p.getDiasCuidados()).isEqualTo(3);
    }

    @Test
    void revivirSinHojasDa402YNoCobra() {
        muerta();
        usuario.setMonedas(249);

        assertThatThrownBy(() -> service.revivir(DUENO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));
        assertThat(usuario.getMonedas()).isEqualTo(249);
        assertThat(plantaRepo.findById(10).get().getEtapaCrecimiento()).isEqualTo("MUERTA");
    }

    @Test
    void soloSeRevivePlantasMuertas() {
        usuario.setMonedas(500);

        assertThatThrownBy(() -> service.revivir(DUENO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(usuario.getMonedas()).isEqualTo(500);
    }

    @Test
    void revivirPlantaAjenaDa404() {
        muerta();
        usuario.setMonedas(500);

        assertThatThrownBy(() -> service.revivir(OTRO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- Recuperacion con abono ---

    private PlantaUsuario enRecuperacion(int salud) {
        PlantaUsuario p = plantaRepo.findById(10).get();
        p.setSaludPorcentaje(salud);
        p.setRecuperacionHasta(LocalDateTime.now().plusHours(20));
        p.setGolpeTipo("OLA_DE_CALOR");
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    @Test
    void unAbonoEnRecuperacionSumaMuchoMasQueUnoNormal() {
        PlantaUsuario p = enRecuperacion(10);

        var dto = service.cuidar(DUENO, 10, "ABONAR");

        assertThat(p.getSaludPorcentaje()).isEqualTo(55);
        assertThat(dto.enRecuperacion()).isTrue();
        assertThat(dto.recuperacionSegundos()).isPositive();
    }

    @Test
    void dosAbonosDejanLaPlantaAlCienYTerminanLaRecuperacion() {
        PlantaUsuario p = enRecuperacion(10);

        service.cuidar(DUENO, 10, "ABONAR");
        var dto = service.cuidar(DUENO, 10, "ABONAR");

        assertThat(p.getSaludPorcentaje()).isEqualTo(100);
        assertThat(p.getRecuperacionHasta()).isNull();
        assertThat(dto.enRecuperacion()).isFalse();
    }

    @Test
    void regarNoRecuperaUnaPlantaGolpeada() {
        PlantaUsuario p = enRecuperacion(10);

        service.cuidar(DUENO, 10, "REGAR");

        assertThat(p.getSaludPorcentaje()).isEqualTo(10);
        assertThat(p.getRecuperacionHasta()).isNotNull();
    }

    @Test
    void sinEventoUnAbonoSigueSumandoDiez() {
        PlantaUsuario p = plantaRepo.findById(10).get();

        service.cuidar(DUENO, 10, "ABONAR");

        assertThat(p.getSaludPorcentaje()).isEqualTo(60);
    }

    @Test
    void noSePuedeCuidarUnaPlantaMuerta() {
        muerta();

        assertThatThrownBy(() -> service.cuidar(DUENO, 10, "ABONAR"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void laAnimacionDelGolpeSoloSeMuestraUnaVez() {
        PlantaUsuario p = enRecuperacion(10);

        assertThat(service.resumen(DUENO, 10).golpeTipo()).isEqualTo("OLA_DE_CALOR");
        var dto = service.golpeVisto(DUENO, 10);

        assertThat(dto.golpeTipo()).isNull();
        assertThat(p.getGolpeTipo()).isNull();
        assertThat(dto.enRecuperacion()).isTrue();
    }

    @Test
    void unaPlantaMuertaInformaCuantoCuestaRevivirla() {
        muerta();

        assertThat(service.resumen(DUENO, 10).costoRevivir()).isEqualTo(250);
    }

    @Test
    void unaPlantaVivaNoTraeCostoDeRevivir() {
        assertThat(service.resumen(DUENO, 10).costoRevivir()).isNull();
    }

    // --- Baul, tope de plantas y apodos ---

    private PlantaUsuario enElBaul() {
        PlantaUsuario p = plantaRepo.findById(10).get();
        p.setEnBaul(true);
        return p;
    }

    @Test
    void guardarEnElBaulCongelaLaPlantaYLaSacaDelJardin() {
        PlantaUsuario p = plantaRepo.findById(10).get();
        p.setEsActiva(true);

        var dto = service.guardarEnBaul(DUENO, 10);

        assertThat(dto.enBaul()).isTrue();
        assertThat(p.getEsActiva()).isFalse();
    }

    @Test
    void unaPlantaGolpeadaNoSePuedeGuardarEnElBaul() {
        plantaRepo.findById(10).get().setRecuperacionHasta(LocalDateTime.now().plusHours(5));

        assertThatThrownBy(() -> service.guardarEnBaul(DUENO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(plantaRepo.findById(10).get().getEnBaul()).isFalse();
    }

    @Test
    void unaPlantaDelBaulNoSeDesgastaNiCambiaConElTiempo() {
        PlantaUsuario p = golpeada(LocalDateTime.of(2026, 11, 16, 12, 0));
        p.setEnBaul(true);

        PlantasService.envejecer(p, LocalDateTime.of(2026, 11, 30, 12, 0));

        assertThat(p.getEtapaCrecimiento()).isEqualTo("BROTE");
        assertThat(p.getHidratacionPorcentaje()).isEqualTo(80);
        assertThat(p.getSaludPorcentaje()).isEqualTo(10);
    }

    @Test
    void unaPlantaDelBaulNoRecibeCuidadosNiProteccionNiSeElige() {
        enElBaul();
        hayDesastreActivo();

        for (Runnable accion : List.<Runnable>of(
                () -> service.cuidar(DUENO, 10, "REGAR"),
                () -> service.prevenir(DUENO, 10),
                () -> service.seleccionar(DUENO, 10))) {
            assertThatThrownBy(accion::run)
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    @Test
    void sacarDelBaulDevuelveLaPlantaYNoCuentaElTiempoGuardado() {
        PlantaUsuario p = enElBaul();
        p.setUpdatedAt(LocalDateTime.now().minusDays(20));
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(DUENO)).thenReturn(List.of(p));

        var dto = service.sacarDelBaul(DUENO, 10);

        assertThat(dto.enBaul()).isFalse();
        assertThat(p.getUpdatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    private List<PlantaUsuario> plantasActivas(int cuantas) {
        List<PlantaUsuario> lista = new java.util.ArrayList<>();
        for (int i = 0; i < cuantas; i++) {
            PlantaUsuario p = new PlantaUsuario();
            p.setIdUsuario(DUENO);
            p.setIdPlantaUsuario(100 + i);
            lista.add(p);
        }
        return lista;
    }

    @Test
    void conElJardinLlenoNoSePuedeSacarOtraDelBaul() {
        PlantaUsuario p = enElBaul();
        List<PlantaUsuario> todas = plantasActivas(12);
        todas.add(p);
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(DUENO)).thenReturn(todas);

        assertThatThrownBy(() -> service.sacarDelBaul(DUENO, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(p.getEnBaul()).isTrue();
    }

    @Test
    void conElJardinLlenoNoSeDigitalizaUnaPlantaNueva() {
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(DUENO)).thenReturn(plantasActivas(12));

        assertThatThrownBy(() -> service.digitalizar(DUENO, 20, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void lasPlantasDelBaulNoCuentanParaElTope() {
        List<PlantaUsuario> lista = plantasActivas(11);
        for (int i = 0; i < 5; i++) {
            PlantaUsuario guardada = new PlantaUsuario();
            guardada.setEnBaul(true);
            lista.add(guardada);
        }

        assertThat(PlantasService.activasDe(lista)).isEqualTo(11);
    }

    @Test
    void elApodoSeCambiaSeRecortaYUnoVacioVuelveAlNombreDeLaEspecie() {
        PlantaUsuario p = plantaRepo.findById(10).get();

        assertThat(service.renombrar(DUENO, 10, "  Mi Monstera  ").name()).isEqualTo("Mi Monstera");
        assertThat(p.getApodo()).isEqualTo("Mi Monstera");
        assertThat(service.renombrar(DUENO, 10, "   ").name()).isEqualTo("Planta");
    }

    @Test
    void unApodoMuyLargoOUnaPlantaAjenaSeRechazan() {
        assertThatThrownBy(() -> service.renombrar(DUENO, 10, "x".repeat(51)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.renombrar(OTRO, 10, "Hola"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void sinMacetaElegidaLaPlantaTieneLaTerracota() {
        assertThat(service.resumen(DUENO, 10).macetaCodigo()).isEqualTo("TERRACOTA");
    }

    @Test
    void laPlantaTraeElCodigoDeSuMacetaPuesta() {
        com.plantia.models.ItemTienda rosa = new com.plantia.models.ItemTienda();
        rosa.setIdItem(77);
        rosa.setCodigo("ROSA");
        when(itemRepo.findById(77)).thenReturn(Optional.of(rosa));
        plantaRepo.findById(10).get().setIdMaceta(77);

        assertThat(service.resumen(DUENO, 10).macetaCodigo()).isEqualTo("ROSA");
    }

    @Test
    void cadaCuidadoUtilSumaSuContadorParaLosLogrosAunqueLasReglasNoPaguen() {
        // Sin reglas cargadas (como pasado el tope diario): no hay XP, pero el cuidado si cuenta
        service.cuidar(DUENO, 10, "REGAR"); // 50 -> 70
        service.cuidar(DUENO, 10, "REGAR"); // 70 -> 90
        service.cuidar(DUENO, 10, "REGAR"); // 90 -> 100
        service.cuidar(DUENO, 10, "REGAR"); // ya al 100: no es un cuidado real, no cuenta
        service.cuidar(DUENO, 10, "SOL");
        service.cuidar(DUENO, 10, "ABONAR");

        assertThat(usuario.getRiegosTotal()).isEqualTo(3);
        assertThat(usuario.getLuzTotal()).isEqualTo(1);
        assertThat(usuario.getAbonosTotal()).isEqualTo(1);
    }

    @Test
    void digitalizarBloqueaAlUsuarioAntesDeGuardarLaPlanta() {
        service.digitalizar(DUENO, null, 5, null);

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(usuarioRepo, plantaRepo);
        orden.verify(usuarioRepo).bloquear(DUENO);
        orden.verify(plantaRepo).save(any());
    }
}
