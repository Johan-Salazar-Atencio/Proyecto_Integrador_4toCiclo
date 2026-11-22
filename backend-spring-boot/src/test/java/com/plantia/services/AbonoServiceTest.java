package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.Usuario;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.UsuarioRepository;

/** El abono de la tienda cuesta monedas y suma medio dia, uno o tres dias de crecimiento, maximo uno al dia. */
class AbonoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private PlantaUsuarioRepository plantaRepo;
    private UsuarioRepository usuarioRepo;
    private ItemTiendaRepository itemRepo;
    private AbonoService service;
    private PlantaUsuario planta;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        plantaRepo = mock(PlantaUsuarioRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        itemRepo = mock(ItemTiendaRepository.class);
        PlantasService plantas = new PlantasService(plantaRepo, null, null, null, null, null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());
        service = new AbonoService(plantaRepo, itemRepo, usuarioRepo, plantas);

        planta = new PlantaUsuario();
        planta.setIdUsuario(1);
        planta.setSaludPorcentaje(80);
        planta.setEtapaCrecimiento("SEMILLA");
        planta.setDiasCuidados(0);
        when(plantaRepo.findById(10)).thenReturn(Optional.of(planta));

        ItemTienda abono = new ItemTienda();
        ReflectionTestUtils.setField(abono, "precioMonedas", 30);
        when(itemRepo.findFirstByTipoAndActivoTrueOrderByPrecioMonedasAsc("ABONO")).thenReturn(Optional.of(abono));

        usuario = new Usuario();
        usuario.setMonedas(100);
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(usuario));
    }

    private void falla(HttpStatus estado, Integer userId) {
        assertThatThrownBy(() -> service.abonar(userId, 10, null, HOY))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(estado));
        verify(usuarioRepo, never()).save(org.mockito.ArgumentMatchers.any());
        verify(plantaRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void abonarCobraYSumaUnDia() {
        service.abonar(1, 10, null, HOY);

        assertThat(usuario.getMonedas()).isEqualTo(70);
        assertThat(planta.getDiasCuidados()).isEqualTo(1);
        assertThat(planta.getUltimoDiaAbonoTienda()).isEqualTo(HOY);
        verify(plantaRepo).save(planta);
    }

    @Test
    void maximoUnAbonoAlDia() {
        service.abonar(1, 10, null, HOY);
        assertThatThrownBy(() -> service.abonar(1, 10, null, HOY))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(usuario.getMonedas()).isEqualTo(70);
        service.abonar(1, 10, null, HOY.plusDays(1));
        assertThat(planta.getDiasCuidados()).isEqualTo(2);
    }

    private ItemTienda abonoConId(int id, String tipo, int precio, int mediosDias) {
        ItemTienda item = new ItemTienda();
        item.setIdItem(id);
        item.setTipo(tipo);
        item.setPrecioMonedas(precio);
        item.setMediosDias(mediosDias);
        when(itemRepo.findById(id)).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void abonoDeMedioDiaGuardaElMedioDiaYElSiguienteLoCompleta() {
        abonoConId(5, "ABONO", 15, 1);
        service.abonar(1, 10, 5, HOY);
        assertThat(planta.getDiasCuidados()).isZero();
        assertThat(planta.isMedioDiaPendiente()).isTrue();
        assertThat(usuario.getMonedas()).isEqualTo(85);

        service.abonar(1, 10, 5, HOY.plusDays(1));
        assertThat(planta.getDiasCuidados()).isEqualTo(1);
        assertThat(planta.isMedioDiaPendiente()).isFalse();
    }

    @Test
    void abonoReforzadoSumaTresDias() {
        abonoConId(6, "ABONO", 80, 6);
        service.abonar(1, 10, 6, HOY);
        assertThat(planta.getDiasCuidados()).isEqualTo(3);
        assertThat(usuario.getMonedas()).isEqualTo(20);
    }

    @Test
    void unItemQueNoEsAbonoDa404() {
        abonoConId(7, "MACETA", 10, 2);
        assertThatThrownBy(() -> service.abonar(1, 10, 7, HOY))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(usuario.getMonedas()).isEqualTo(100);
    }

    @Test
    void sinMonedasDa402() {
        usuario.setMonedas(10);
        falla(HttpStatus.PAYMENT_REQUIRED, 1);
        assertThat(planta.getDiasCuidados()).isZero();
    }

    @Test
    void plantaAjenaDa404() {
        falla(HttpStatus.NOT_FOUND, 2);
    }

    @Test
    void plantaMarchitaOMuertaDa409() {
        planta.setSaludPorcentaje(20);
        falla(HttpStatus.CONFLICT, 1);
        planta.setSaludPorcentaje(80);
        planta.setEtapaCrecimiento("MUERTA");
        falla(HttpStatus.CONFLICT, 1);
    }

    @Test
    void plantaQueYaCrecioDelTodoDa409YNoCobra() {
        planta.setDiasCuidados(7); // sin forma: 7 dias
        falla(HttpStatus.CONFLICT, 1);
        assertThat(usuario.getMonedas()).isEqualTo(100);
    }

    @Test
    void elAbonoDeTiendaCuentaParaElLogroDeAbonos() {
        service.abonar(1, 10, null, HOY);

        assertThat(usuario.getAbonosTotal()).isEqualTo(1);
    }

    @Test
    void unAbonoRechazadoNoCuenta() {
        service.abonar(1, 10, null, HOY);
        assertThatThrownBy(() -> service.abonar(1, 10, null, HOY)).isInstanceOf(ResponseStatusException.class);

        assertThat(usuario.getAbonosTotal()).isEqualTo(1);
    }
}
