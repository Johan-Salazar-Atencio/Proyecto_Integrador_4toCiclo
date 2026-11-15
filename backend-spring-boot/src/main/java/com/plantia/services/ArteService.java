package com.plantia.services;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/** Arte de la escena: fondos por momento del dia y animaciones de desastre (solo lectura). */
@Service
public class ArteService {

    private final FondoEscenaRepository fondoRepo;
    private final AnimacionDesastreRepository animacionRepo;
    private final FrameAnimacionRepository frameRepo;
    private final TipoDesastreRepository tipoRepo;

    public ArteService(FondoEscenaRepository fondoRepo, AnimacionDesastreRepository animacionRepo,
                       FrameAnimacionRepository frameRepo, TipoDesastreRepository tipoRepo) {
        this.fondoRepo = fondoRepo;
        this.animacionRepo = animacionRepo;
        this.frameRepo = frameRepo;
        this.tipoRepo = tipoRepo;
    }

    /** Fondos activos; dentro de cada momento, el predeterminado va primero. */
    @Transactional(readOnly = true)
    public List<FondoDto> fondos() {
        return fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc().stream()
                .map(f -> aDto(f, false))
                .toList();
    }

    /**
     * Fondos que le tocan a un usuario: los predeterminados y los de la tienda que tiene equipados.
     * Dentro de cada momento va primero el equipado y luego el predeterminado.
     */
    @Transactional(readOnly = true)
    public List<FondoDto> fondos(java.util.Set<Integer> equipados) {
        return fondoRepo.findByActivoTrueOrderByMomentoAscEsPredeterminadoDescIdFondoAsc().stream()
                .filter(f -> Boolean.TRUE.equals(f.getEsPredeterminado()) || equipados.contains(f.getIdFondo()))
                .sorted(Comparator.comparing(FondoEscena::getMomento)
                        .thenComparing(f -> !equipados.contains(f.getIdFondo())))
                .map(f -> aDto(f, equipados.contains(f.getIdFondo())))
                .toList();
    }

    /** Animaciones activas con sus frames en orden; las que no tienen frames no se envian. */
    @Transactional(readOnly = true)
    public List<AnimacionDto> animaciones() {
        List<AnimacionDesastre> animaciones = animacionRepo.findByActivaTrueOrderByIdTipoDesastreAscFaseAsc();
        if (animaciones.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> codigos = tipoRepo.findAllById(
                        animaciones.stream().map(AnimacionDesastre::getIdTipoDesastre).distinct().toList()).stream()
                .collect(Collectors.toMap(TipoDesastre::getIdTipoDesastre, TipoDesastre::getCodigo));
        Map<Integer, List<String>> frames = frameRepo.findByIdAnimacionInOrderByIdAnimacionAscOrdenAsc(
                        animaciones.stream().map(AnimacionDesastre::getIdAnimacion).toList()).stream()
                .collect(Collectors.groupingBy(FrameAnimacion::getIdAnimacion,
                        Collectors.mapping(FrameAnimacion::getImagenUrl, Collectors.toList())));
        return animaciones.stream()
                .filter(a -> codigos.containsKey(a.getIdTipoDesastre()) && frames.containsKey(a.getIdAnimacion()))
                .sorted(Comparator.comparing((AnimacionDesastre a) -> codigos.get(a.getIdTipoDesastre()))
                        .thenComparing(AnimacionDesastre::getFase))
                .map(a -> new AnimacionDto(
                        codigos.get(a.getIdTipoDesastre()),
                        a.getFase(),
                        a.getMsPorFrame(),
                        Boolean.TRUE.equals(a.getRepetir()),
                        frames.get(a.getIdAnimacion())))
                .toList();
    }

    static FondoDto aDto(FondoEscena f, boolean equipado) {
        int frames = f.getTotalFrames() != null && f.getTotalFrames() > 0 ? f.getTotalFrames() : 1;
        return new FondoDto(
                "fondo_" + f.getIdFondo(),
                f.getMomento(),
                f.getNombre(),
                f.getImagenUrl(),
                frames,
                f.getAnchoPx() != null ? f.getAnchoPx() : 0,
                f.getAltoPx() != null ? f.getAltoPx() : 0,
                f.getMsPorFrame() != null ? f.getMsPorFrame() : 200,
                Boolean.TRUE.equals(f.getEsPredeterminado()),
                equipado);
    }
}
