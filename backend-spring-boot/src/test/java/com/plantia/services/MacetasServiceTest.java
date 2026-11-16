package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;

/** Aplicar una maceta que ya se tiene a las plantas elegidas: todo o nada. */
class MacetasServiceTest {

    private static final int USER = 1;

    private PlantaUsuarioRepository plantaRepo;
    private InventarioUsuarioRepository inventarioRepo;
    private ItemTiendaRepository itemRepo;
    private PlantasService plantas;
    private MacetasService service;
    private PlantaUsuario p1;
    private PlantaUsuario p2;

    private static ItemTienda maceta(int id, int precio, boolean activo) {
        ItemTienda i = new ItemTienda();
        i.setIdItem(id);
        i.setTipo("MACETA");
        i.setPrecioMonedas(precio);
        i.setActivo(activo);
        i.setCodigo("M" + id);
        return i;
    }

    private static PlantaUsuario planta(int id, int dueno) {
        PlantaUsuario p = new PlantaUsuario();
        p.setIdPlantaUsuario(id);
        p.setIdUsuario(dueno);
        return p;
    }

    @BeforeEach
    void preparar() {
        plantaRepo = mock(PlantaUsuarioRepository.class);
        inventarioRepo = mock(InventarioUsuarioRepository.class);
        itemRepo = mock(ItemTiendaRepository.class);
        plantas = mock(PlantasService.class);
        service = new MacetasService(itemRepo, inventarioRepo, plantaRepo, plantas);

        p1 = planta(1, USER);
        p2 = planta(2, USER);
        when(plantaRepo.findById(1)).thenReturn(Optional.of(p1));
        when(plantaRepo.findById(2)).thenReturn(Optional.of(p2));
        when(plantaRepo.findById(3)).thenReturn(Optional.of(planta(3, 99)));
        when(inventarioRepo.findByIdUsuario(USER)).thenReturn(List.of());
        when(plantas.resumen(any(), any())).thenReturn(new PlantSummaryDto("Planta", "Especie", 1, 1, 1, "hoy", null));
        when(itemRepo.findById(10)).thenReturn(Optional.of(maceta(10, 0, true)));      // gratis
        when(itemRepo.findById(11)).thenReturn(Optional.of(maceta(11, 80, true)));     // de pago
        when(itemRepo.findById(12)).thenReturn(Optional.of(maceta(12, 0, false)));     // desactivada
    }

    @Test
    void aplicaUnaMacetaGratisAVariasPlantas() {
        var resultado = service.aplicar(USER, 10, List.of(1, 2, 2));

        assertThat(resultado).hasSize(2);
        assertThat(p1.getIdMaceta()).isEqualTo(10);
        assertThat(p2.getIdMaceta()).isEqualTo(10);
    }

    @Test
    void unaMacetaDePagoSoloSiLaCompro() {
        assertThatThrownBy(() -> service.aplicar(USER, 11, List.of(1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(p1.getIdMaceta()).isNull();

        InventarioUsuario compra = new InventarioUsuario();
        compra.setIdItem(11);
        when(inventarioRepo.findByIdUsuario(USER)).thenReturn(List.of(compra));
        service.aplicar(USER, 11, List.of(1));

        assertThat(p1.getIdMaceta()).isEqualTo(11);
    }

    @Test
    void unaPlantaAjenaOUnaMacetaQueNoExisteDan404YNoCambiaNada() {
        assertThatThrownBy(() -> service.aplicar(USER, 10, List.of(1, 3)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.aplicar(USER, 12, List.of(1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(p1.getIdMaceta()).isNull();
        verify(plantaRepo, never()).saveAll(any());
    }

    @Test
    void unaPlantaDelBaulNoRecibeMaceta() {
        p2.setEnBaul(true);

        assertThatThrownBy(() -> service.aplicar(USER, 10, List.of(1, 2)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(p1.getIdMaceta()).isNull();
    }

    @Test
    void sinPlantasElegidasDa400() {
        assertThatThrownBy(() -> service.aplicar(USER, 10, List.of()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
