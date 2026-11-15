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

    @BeforeEach
    void preparar() {
        fondoRepo = mock(FondoEscenaRepository.class);
        animacionRepo = mock(AnimacionDesastreRepository.class);
        frameRepo = mock(FrameAnimacionRepository.class);
        tipoRepo = mock(TipoDesastreRepository.class);
        service = new ArteService(fondoRepo, animacionRepo, frameRepo, tipoRepo);
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

    @Test
    void losFondosDeUnUsuarioSonLosPredeterminadosYLosEquipados() {
        FondoEscena normal = fondo(1, "DIA", 96, true);
        FondoEscena comprado = fondo(2, "DIA", 96, false);
        FondoEscena ajeno = fondo(3, "NOCHE", 96, false);
        when(fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc())
                .thenReturn(List.of(normal, comprado, ajeno));

        List<FondoDto> fondos = service.fondos(java.util.Set.of(2));

        // El equipado va primero dentro de su momento y el que no es suyo no se envia
        assertThat(fondos).extracting(FondoDto::id).containsExactly("fondo_2", "fondo_1");
        assertThat(fondos.get(0).equipado()).isTrue();
        assertThat(fondos.get(1).equipado()).isFalse();
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
