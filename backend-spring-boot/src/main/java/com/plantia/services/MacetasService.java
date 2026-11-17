package com.plantia.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;

/**
 * Pone una maceta (que el usuario ya tiene) a las plantas que elija. Las macetas se compran una vez en la tienda;
 * aplicarlas a las plantas es gratis y se puede repetir. Las plantas del baul no se tocan.
 */
@Service
public class MacetasService {

    static final int MAX_PLANTAS = 50;

    private final ItemTiendaRepository itemRepo;
    private final InventarioUsuarioRepository inventarioRepo;
    private final PlantaUsuarioRepository plantaRepo;
    private final PlantasService plantas;

    public MacetasService(ItemTiendaRepository itemRepo, InventarioUsuarioRepository inventarioRepo,
                          PlantaUsuarioRepository plantaRepo, PlantasService plantas) {
        this.itemRepo = itemRepo;
        this.inventarioRepo = inventarioRepo;
        this.plantaRepo = plantaRepo;
        this.plantas = plantas;
    }

    /**
     * 404 si la maceta o una planta no existen (o no son suyas); 409 si no tiene esa maceta o una planta esta en el
     * baul; 400 si la lista esta vacia o es demasiado larga. Falla entera: no se aplica a medias.
     */
    @Transactional
    public List<PlantSummaryDto> aplicar(Integer userId, Integer idItem, List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_PLANTAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige entre 1 y " + MAX_PLANTAS + " plantas");
        }
        ItemTienda maceta = itemRepo.findById(idItem)
                .filter(i -> Boolean.TRUE.equals(i.getActivo()) && TiendaService.MACETA.equals(i.getTipo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maceta no encontrada"));
        List<Integer> comprados = inventarioRepo.findByIdUsuario(userId).stream().map(InventarioUsuario::getIdItem).toList();
        if (!TiendaService.esSuyo(maceta, comprados)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primero compra esta maceta en la tienda");
        }
        List<PlantaUsuario> elegidas = ids.stream().distinct()
                .map(id -> plantaRepo.findById(id)
                        .filter(p -> userId.equals(p.getIdUsuario()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planta no encontrada")))
                .toList();
        if (elegidas.stream().anyMatch(PlantasService::enBaul)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Una de las plantas esta en el baul");
        }
        elegidas.forEach(p -> p.setIdMaceta(maceta.getIdItem()));
        plantaRepo.saveAll(elegidas);
        return elegidas.stream().map(p -> plantas.resumen(userId, p.getIdPlantaUsuario())).toList();
    }
}
