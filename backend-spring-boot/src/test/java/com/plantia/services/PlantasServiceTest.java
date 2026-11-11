package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.models.EscaneoHistorial;
import com.plantia.models.PlantaUsuario;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.repositories.PlantaUsuarioRepository;

/** Un usuario no puede tocar plantas ni escaneos de otro (antes bastaba con cambiar el id de la URL). */
class PlantasServiceTest {

    private static final int DUENO = 1;
    private static final int OTRO = 2;

    private PlantaUsuarioRepository plantaRepo;
    private EscaneoHistorialRepository escaneoRepo;
    private PlantasService service;

    @BeforeEach
    void preparar() {
        plantaRepo = mock(PlantaUsuarioRepository.class);
        escaneoRepo = mock(EscaneoHistorialRepository.class);
        service = new PlantasService(plantaRepo, mock(PlantaCatalogoRepository.class), escaneoRepo);

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
        assertThatThrownBy(() -> service.crearDesdeEscaneo(OTRO, 20))
                .isInstanceOf(ResponseStatusException.class);
        verify(plantaRepo, never()).save(any());
    }

    @Test
    void elDuenoSiPuedeRegar() {
        assertThat(service.cuidar(DUENO, 10, "REGAR")).isNotNull();
        verify(plantaRepo).save(any());
    }
}
