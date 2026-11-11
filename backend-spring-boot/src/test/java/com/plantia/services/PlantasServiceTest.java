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

import com.fasterxml.jackson.databind.ObjectMapper;
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
        service = new PlantasService(plantaRepo, mock(PlantaCatalogoRepository.class), escaneoRepo, new ObjectMapper());

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
    void cuidarUtilHaceCrecerYCambiaLaEtapa() {
        for (int i = 0; i < 2; i++) {
            service.cuidar(DUENO, 10, "ABONAR");
        }
        var dto = service.cuidar(DUENO, 10, "SOL");
        assertThat(dto.crecimiento()).isEqualTo(3);
        assertThat(PlantasService.etapaSegun(3)).isEqualTo("BROTE");
        assertThat(PlantasService.etapaSegun(9)).isEqualTo("ADULTA");
    }
}
