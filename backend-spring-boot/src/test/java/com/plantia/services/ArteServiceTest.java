package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.plantia.dto.AnimacionDto;
import com.plantia.dto.FondoDto;
import com.plantia.models.AnimacionDesastre;
import com.plantia.models.FondoEscena;
import com.plantia.models.PlantaUsuario;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.models.FrameAnimacion;
import com.plantia.models.TipoDesastre;
import com.plantia.repositories.AnimacionDesastreRepository;
import com.plantia.repositories.FondoEscenaRepository;
import com.plantia.repositories.FrameAnimacionRepository;
import com.plantia.repositories.TipoDesastreRepository;

/** Fondos y animaciones de desastre: solo lectura, con sus frames en orden. */
class ArteServiceTest {

    private FondoEscenaRepository fondoRepo;
    private AnimacionDesastreRepository animacionRepo;
    private FrameAnimacionRepository frameRepo;
    private TipoDesastreRepository tipoRepo;
    private ArteService service;
    private PlantaUsuarioRepository plantaRepo;

    @BeforeEach
    void preparar() {
        fondoRepo = mock(FondoEscenaRepository.class);
        animacionRepo = mock(AnimacionDesastreRepository.class);
        frameRepo = mock(FrameAnimacionRepository.class);
        tipoRepo = mock(TipoDesastreRepository.class);
        plantaRepo = mock(PlantaUsuarioRepository.class);
        service = new ArteService(fondoRepo, animacionRepo, frameRepo, tipoRepo, plantaRepo);
    }

    private static FondoEscena fondo(int id, String momento, int frames, boolean predeterminado) {
        FondoEscena f = new FondoEscena();
        f.setIdFondo(id);
        f.setMomento(momento);
        f.setNombre("Fondo " + id);
        f.setImagenUrl("/files/arte/fondos/" + momento.toLowerCase() + ".png");
        f.setTotalFrames(frames);
        f.setAnchoPx(96);
        f.setAltoPx(96);
        f.setMsPorFrame(250);
        f.setEsPredeterminado(predeterminado);
        return f;
    }

    private static TipoDesastre tipo(int id, String codigo) {
        TipoDesastre t = new TipoDesastre();
        t.setIdTipoDesastre(id);
        t.setCodigo(codigo);
        return t;
    }

    private static AnimacionDesastre animacion(int id, int idTipo, String fase, int ms) {
        AnimacionDesastre a = new AnimacionDesastre();
        a.setIdAnimacion(id);
        a.setIdTipoDesastre(idTipo);
        a.setFase(fase);
        a.setMsPorFrame(ms);
        a.setRepetir(true);
        return a;
    }

    private static FrameAnimacion frame(int idAnimacion, int orden) {
        FrameAnimacion f = new FrameAnimacion();
        f.setIdAnimacion(idAnimacion);
        f.setOrden(orden);
        f.setImagenUrl("/files/arte/desastres/x_" + idAnimacion + "_" + orden + ".png");
        return f;
    }

    @Test
    void losFondosSalenConSuTiraYSuTamano() {
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc())
                .thenReturn(List.of(fondo(1, "DIA", 8, true)));

        FondoDto dto = service.fondos().get(0);

        assertThat(dto.momento()).isEqualTo("DIA");
        assertThat(dto.frames()).isEqualTo(8);
        assertThat(dto.widthPx()).isEqualTo(96);
        assertThat(dto.msPerFrame()).isEqualTo(250);
        assertThat(dto.isDefault()).isTrue();
        assertThat(dto.imageUrl()).startsWith("/files/arte/fondos/");
    }

    private static FondoEscena escena(int id, String momento, Integer idTema, boolean predeterminado) {
        FondoEscena f = fondo(id, momento, 96, predeterminado);
        f.setIdTema(idTema);
        return f;
    }

    private static PlantaUsuario planta(Integer idTema, boolean activa) {
        PlantaUsuario p = new PlantaUsuario();
        p.setIdTemaFondo(idTema);
        p.setEsActiva(activa);
        p.setEtapaCrecimiento("SEMILLA");
        return p;
    }

    private List<FondoEscena> catalogo() {
        // Tema 1 = "Campo" (predeterminado); tema 7 = completo; tema 8 = solo trae la noche
        return List.of(
                escena(1, "DIA", 1, true), escena(2, "TARDE", 1, true), escena(3, "NOCHE", 1, true),
                escena(10, "DIA", 7, false), escena(11, "TARDE", 7, false), escena(12, "NOCHE", 7, false),
                escena(20, "NOCHE", 8, false));
    }

    @Test
    void losFondosSonLosDelTemaDeLaPlantaActivaUnoPorMomento() {
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc()).thenReturn(catalogo());
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(5)).thenReturn(List.of(planta(null, false), planta(7, true)));

        List<FondoDto> fondos = service.fondos(5);

        assertThat(fondos).extracting(FondoDto::id).containsExactly("fondo_10", "fondo_11", "fondo_12");
        assertThat(fondos).allMatch(FondoDto::equipado);
    }

    @Test
    void sinTemaElegidoSeUsaElPredeterminado() {
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc()).thenReturn(catalogo());
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(5)).thenReturn(List.of(planta(null, true)));

        List<FondoDto> fondos = service.fondos(5);

        assertThat(fondos).extracting(FondoDto::id).containsExactly("fondo_1", "fondo_2", "fondo_3");
        assertThat(fondos).noneMatch(FondoDto::equipado);
    }

    @Test
    void siElTemaNoTraeUnMomentoEseUsaElPredeterminado() {
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc()).thenReturn(catalogo());
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(5)).thenReturn(List.of(planta(8, true)));

        assertThat(service.fondos(5)).extracting(FondoDto::id).containsExactly("fondo_1", "fondo_2", "fondo_20");
    }

    @Test
    void sinPlantasSeUsaElPredeterminadoYUnaMuertaNoCuenta() {
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc()).thenReturn(catalogo());
        PlantaUsuario muerta = planta(7, true);
        muerta.setEtapaCrecimiento("MUERTA");
        when(plantaRepo.findByIdUsuarioOrderByFechaPlantadoDesc(5)).thenReturn(List.of(muerta));

        assertThat(service.fondos(5)).extracting(FondoDto::id).containsExactly("fondo_1", "fondo_2", "fondo_3");
    }

    @Test
    void unFondoSinFramesNiTamanoNoRompeNiDividePorCero() {
        FondoEscena f = fondo(2, "NOCHE", 0, false);
        f.setAnchoPx(null);
        f.setMsPorFrame(null);
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc()).thenReturn(List.of(f));

        FondoDto dto = service.fondos().get(0);

        assertThat(dto.frames()).isEqualTo(1);
        assertThat(dto.widthPx()).isZero();
        assertThat(dto.msPerFrame()).isEqualTo(200);
    }

    @Test
    void lasAnimacionesTraenElCodigoDelTipoYSusFramesEnOrden() {
        when(animacionRepo.findByActivaTrueOrderByIdTipoDesastreAscFaseAsc())
                .thenReturn(List.of(animacion(10, 1, "PICO", 150)));
        when(tipoRepo.findAllById(List.of(1))).thenReturn(List.of(tipo(1, "HUAYCO")));
        when(frameRepo.findByIdAnimacionInOrderByIdAnimacionAscOrdenAsc(List.of(10)))
                .thenReturn(List.of(frame(10, 1), frame(10, 2), frame(10, 3)));

        List<AnimacionDto> animaciones = service.animaciones();

        assertThat(animaciones).hasSize(1);
        assertThat(animaciones.get(0).disasterType()).isEqualTo("HUAYCO");
        assertThat(animaciones.get(0).phase()).isEqualTo("PICO");
        assertThat(animaciones.get(0).msPerFrame()).isEqualTo(150);
        assertThat(animaciones.get(0).repeat()).isTrue();
        assertThat(animaciones.get(0).frames()).hasSize(3).first().asString().endsWith("_10_1.png");
    }

    @Test
    void unaAnimacionSinFramesNoSeEnvia() {
        when(animacionRepo.findByActivaTrueOrderByIdTipoDesastreAscFaseAsc())
                .thenReturn(List.of(animacion(10, 1, "PICO", 150), animacion(11, 1, "AVISO", 300)));
        when(tipoRepo.findAllById(List.of(1))).thenReturn(List.of(tipo(1, "HELADA")));
        when(frameRepo.findByIdAnimacionInOrderByIdAnimacionAscOrdenAsc(List.of(10, 11)))
                .thenReturn(List.of(frame(11, 1)));

        assertThat(service.animaciones()).extracting(AnimacionDto::phase).containsExactly("AVISO");
    }

    @Test
    void sinAnimacionesActivasDevuelveListaVaciaSinConsultarFrames() {
        when(animacionRepo.findByActivaTrueOrderByIdTipoDesastreAscFaseAsc()).thenReturn(List.of());

        assertThat(service.animaciones()).isEmpty();
    }
}
