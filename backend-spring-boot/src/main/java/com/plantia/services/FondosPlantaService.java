package com.plantia.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.TemaFondoDto;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.TemaFondo;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.TemaFondoRepository;

/**
 * Fondos de una planta: los temas que el usuario puede ponerle (el predeterminado y los que compro) y
 * el que tiene puesto. El fondo es de cada planta; las demas no cambian.
 */
@Service
public class FondosPlantaService {

    private final PlantaUsuarioRepository plantaRepo;
    private final TemaFondoRepository temaRepo;
    private final InventarioUsuarioRepository inventarioRepo;
    private final ItemTiendaRepository itemRepo;
    private final TiendaService tienda;

    public FondosPlantaService(PlantaUsuarioRepository plantaRepo, TemaFondoRepository temaRepo,
                               InventarioUsuarioRepository inventarioRepo, ItemTiendaRepository itemRepo,
                               TiendaService tienda) {
        this.plantaRepo = plantaRepo;
        this.temaRepo = temaRepo;
        this.inventarioRepo = inventarioRepo;
        this.itemRepo = itemRepo;
        this.tienda = tienda;
    }

    /** Temas disponibles para esa planta: primero el predeterminado y luego los comprados. */
    @Transactional(readOnly = true)
    public List<TemaFondoDto> temas(Integer userId, Integer idPlanta) {
        PlantaUsuario planta = propia(userId, idPlanta);
        TemaFondo predeterminado = predeterminado();
        List<TemaFondo> temas = new ArrayList<>();
        temas.add(predeterminado);
        temas.addAll(temasComprados(userId).stream().filter(t -> !t.getIdTema().equals(predeterminado.getIdTema())).toList());
        Map<Integer, List<TemaFondoDto.Variante>> variantes = tienda.variantesDe(temas.stream().map(TemaFondo::getIdTema).toList());
        Integer enUso = planta.getIdTemaFondo() != null ? planta.getIdTemaFondo() : predeterminado.getIdTema();
        return temas.stream()
                .map(t -> new TemaFondoDto(t.getIdTema(), t.getNombre(), t.getDescripcion() != null ? t.getDescripcion() : "",
                        Boolean.TRUE.equals(t.getEsPredeterminado()), t.getIdTema().equals(enUso),
                        variantes.getOrDefault(t.getIdTema(), List.of())))
                .toList();
    }

    /** Pone un tema a la planta; null o el predeterminado la devuelven al fondo de siempre. 409 si no lo tiene. */
    @Transactional
    public List<TemaFondoDto> aplicar(Integer userId, Integer idPlanta, Integer idTema) {
        PlantaUsuario planta = propia(userId, idPlanta);
        TemaFondo predeterminado = predeterminado();
        if (idTema == null || idTema.equals(predeterminado.getIdTema())) {
            planta.setIdTemaFondo(null);
        } else if (temasComprados(userId).stream().anyMatch(t -> idTema.equals(t.getIdTema()))) {
            planta.setIdTemaFondo(idTema);
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No tienes ese fondo");
        }
        plantaRepo.save(planta);
        return temas(userId, idPlanta);
    }

    /** Temas de los articulos que compro el usuario (siguen siendo suyos aunque el admin desactive el articulo). */
    private List<TemaFondo> temasComprados(Integer userId) {
        List<Integer> items = inventarioRepo.findByIdUsuario(userId).stream().map(InventarioUsuario::getIdItem).toList();
        if (items.isEmpty()) {
            return List.of();
        }
        Set<Integer> ids = new HashSet<>(itemRepo.findAllById(items).stream()
                .map(ItemTienda::getIdTema).filter(Objects::nonNull).collect(Collectors.toSet()));
        return temaRepo.findAllById(ids).stream()
                .sorted(java.util.Comparator.comparing(TemaFondo::getIdTema))
                .toList();
    }

    private TemaFondo predeterminado() {
        return temaRepo.findByEsPredeterminadoTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta el tema de fondo predeterminado"));
    }

    private PlantaUsuario propia(Integer userId, Integer idPlanta) {
        return plantaRepo.findById(idPlanta)
                .filter(p -> userId.equals(p.getIdUsuario()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planta no encontrada"));
    }
}
