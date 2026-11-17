package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.TemaFondoDto;
import com.plantia.models.FondoEscena;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.TemaFondo;
import com.plantia.repositories.FondoEscenaRepository;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.TemaFondoRepository;
import com.plantia.repositories.UsuarioRepository;

/** El fondo es de cada planta: solo se pueden poner los temas comprados y el predeterminado. */
class FondosPlantaServiceTest {

    private static final int DUENO = 1;
    private static final int OTRO = 2;
    private static final int PLANTA = 10;

    private PlantaUsuarioRepository plantaRepo;
    private FondosPlantaService service;
    private PlantaUsuario planta;
    private final List<InventarioUsuario> inventario = new ArrayList<>();

    private static TemaFondo tema(int id, String nombre, boolean predeterminado) {
        TemaFondo t = new TemaFondo();
        t.setIdTema(id);
        t.setNombre(nombre);
        t.setEsPredeterminado(predeterminado);
        return t;
    }

    private static InventarioUsuario compra(int idItem) {
        InventarioUsuario i = new InventarioUsuario();
        i.setIdUsuario(DUENO);
        i.setIdItem(idItem);
        return i;
    }

    @BeforeEach
    void preparar() {
        plantaRepo = mock(PlantaUsuarioRepository.class);
        TemaFondoRepository temaRepo = mock(TemaFondoRepository.class);
        InventarioUsuarioRepository inventarioRepo = mock(InventarioUsuarioRepository.class);
        ItemTiendaRepository itemRepo = mock(ItemTiendaRepository.class);
        FondoEscenaRepository fondoRepo = mock(FondoEscenaRepository.class);
        TiendaService tienda = new TiendaService(itemRepo, inventarioRepo, fondoRepo, mock(UsuarioRepository.class));
        service = new FondosPlantaService(plantaRepo, temaRepo, inventarioRepo, itemRepo, tienda);

        planta = new PlantaUsuario();
        planta.setIdUsuario(DUENO);
        when(plantaRepo.findById(PLANTA)).thenReturn(Optional.of(planta));

        TemaFondo campo = tema(1, "Campo", true);
        TemaFondo nevado = tema(20, "Nevado", false);
        TemaFondo cyber = tema(22, "Cyberpunk", false);
        when(temaRepo.findByEsPredeterminadoTrue()).thenReturn(Optional.of(campo));
        when(temaRepo.findAllById(any())).thenAnswer(inv -> {
            List<Integer> ids = new ArrayList<>();
            ((Iterable<Integer>) inv.getArgument(0)).forEach(ids::add);
            return List.of(campo, nevado, cyber).stream().filter(t -> ids.contains(t.getIdTema())).toList();
        });

        // El usuario compro el articulo 2 (tema Nevado); el 4 (Cyberpunk) no
        ItemTienda artNevado = new ItemTienda();
        artNevado.setIdItem(2);
        artNevado.setIdTema(20);
        when(inventarioRepo.findByIdUsuario(DUENO)).thenAnswer(i -> new ArrayList<>(inventario));
        when(itemRepo.findAllById(any())).thenReturn(List.of(artNevado));
        inventario.add(compra(2));

        FondoEscena dia = new FondoEscena();
        dia.setIdTema(1);
        dia.setMomento("DIA");
        dia.setImagenUrl("/x.png");
        when(fondoRepo.findByIdTemaInAndActivoTrue(any())).thenReturn(List.of(dia));
    }

    @Test
    void sinTemaElegidoLaPlantaUsaElPredeterminadoYVenSusFondos() {
        List<TemaFondoDto> temas = service.temas(DUENO, PLANTA);

        assertThat(temas).extracting(TemaFondoDto::nombre).containsExactly("Campo", "Nevado");
        assertThat(temas.get(0).predeterminado()).isTrue();
        assertThat(temas.get(0).enUso()).isTrue();
        assertThat(temas.get(1).enUso()).isFalse();
    }

    @Test
    void ponerUnTemaComprado() {
        List<TemaFondoDto> temas = service.aplicar(DUENO, PLANTA, 20);

        assertThat(planta.getIdTemaFondo()).isEqualTo(20);
        assertThat(temas).filteredOn(TemaFondoDto::enUso).extracting(TemaFondoDto::nombre).containsExactly("Nevado");
        verify(plantaRepo).save(planta);
    }

    @Test
    void volverAlPredeterminadoConNuloOConSuId() {
        planta.setIdTemaFondo(20);
        service.aplicar(DUENO, PLANTA, null);
        assertThat(planta.getIdTemaFondo()).isNull();

        planta.setIdTemaFondo(20);
        service.aplicar(DUENO, PLANTA, 1);
        assertThat(planta.getIdTemaFondo()).isNull();
    }

    @Test
    void unTemaQueNoComproDa409YNoCambiaNada() {
        planta.setIdTemaFondo(20);

        assertThatThrownBy(() -> service.aplicar(DUENO, PLANTA, 22))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(planta.getIdTemaFondo()).isEqualTo(20);
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void unaPlantaAjenaDa404() {
        assertThatThrownBy(() -> service.temas(OTRO, PLANTA))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.aplicar(OTRO, PLANTA, 20))
                .isInstanceOf(ResponseStatusException.class);
        verify(plantaRepo, never()).save(any());
    }
}
