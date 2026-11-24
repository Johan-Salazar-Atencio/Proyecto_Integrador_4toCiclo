package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.AbonosDto;
import com.plantia.models.AbonoUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.Usuario;
import com.plantia.repositories.AbonoUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.UsuarioRepository;

/** Abonos como inventario: comprar suma al contador, usar descuenta 1 y suma dias (maximo uno al dia por planta). */
class AbonoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final int SUAVE = 5;
    private static final int ORGANICO = 6;
    private static final int REFORZADO = 7;

    private PlantaUsuarioRepository plantaRepo;
    private UsuarioRepository usuarioRepo;
    private ItemTiendaRepository itemRepo;
    private AbonoUsuarioRepository abonoRepo;
    private AbonoService service;
    private PlantaUsuario planta;
    private Usuario usuario;
    /** Existencias simuladas del usuario 1 por articulo. */
    private final Map<Integer, Integer> stock = new HashMap<>();

    @BeforeEach
    void preparar() {
        plantaRepo = mock(PlantaUsuarioRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        itemRepo = mock(ItemTiendaRepository.class);
        abonoRepo = mock(AbonoUsuarioRepository.class);
        PlantasService plantas = new PlantasService(plantaRepo, null, null, null, null, null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());
        service = new AbonoService(plantaRepo, itemRepo, usuarioRepo, plantas, abonoRepo);

        planta = new PlantaUsuario();
        planta.setIdUsuario(1);
        planta.setSaludPorcentaje(80);
        planta.setEtapaCrecimiento("SEMILLA");
        planta.setDiasCuidados(0);
        when(plantaRepo.findById(10)).thenReturn(Optional.of(planta));

        abono(SUAVE, "ABONO_MEDIO", 15, 1);
        abono(ORGANICO, "ABONO_1", 30, 2);
        abono(REFORZADO, "ABONO_3", 80, 6);

        usuario = new Usuario();
        usuario.setMonedas(100);
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(usuario));

        // Contador en memoria: sumar/descontar igual que las consultas SQL
        doAnswer(i -> {
            stock.merge(i.<Integer>getArgument(1), i.<Integer>getArgument(2), Integer::sum);
            return 1;
        }).when(abonoRepo).sumar(anyInt(), anyInt(), anyInt());
        doAnswer(i -> {
            int id = i.<Integer>getArgument(1);
            int actual = stock.getOrDefault(id, 0);
            if (actual <= 0) {
                return 0;
            }
            stock.put(id, actual - 1);
            return 1;
        }).when(abonoRepo).descontarUno(anyInt(), anyInt());
        when(abonoRepo.findByIdUsuario(1)).thenAnswer(i -> stock.entrySet().stream().map(e -> {
            AbonoUsuario a = new AbonoUsuario();
            a.setIdUsuario(1);
            a.setIdItem(e.getKey());
            a.setCantidad(e.getValue());
            return a;
        }).toList());
    }

    private ItemTienda abono(int id, String codigo, int precio, int mediosDias) {
        ItemTienda item = new ItemTienda();
        item.setIdItem(id);
        item.setTipo("ABONO");
        item.setCodigo(codigo);
        item.setNombre("Abono " + codigo);
        item.setPrecioMonedas(precio);
        item.setMediosDias(mediosDias);
        when(itemRepo.findById(id)).thenReturn(Optional.of(item));
        return item;
    }

    private void listarItems() {
        List<ItemTienda> items = List.of(itemRepo.findById(SUAVE).get(), itemRepo.findById(ORGANICO).get(),
                itemRepo.findById(REFORZADO).get());
        when(itemRepo.findByTipoOrderByPrecioMonedasAscIdItemAsc("ABONO")).thenReturn(items);
    }

    private void falla(HttpStatus estado, Runnable accion) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(estado));
    }

    private int existencias(int idItem) {
        return stock.getOrDefault(idItem, 0);
    }

    // ---- comprar ----

    @Test
    void comprarSumaAlContadorYCobraSinUsarlo() {
        service.comprar(1, ORGANICO, null);

        assertThat(existencias(ORGANICO)).isEqualTo(1);
        assertThat(usuario.getMonedas()).isEqualTo(70);
        assertThat(planta.getDiasCuidados()).isZero();
        assertThat(planta.getUltimoDiaAbonoTienda()).isNull();
        verify(plantaRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void comprarVariosCobraPorTodosYSuma() {
        service.comprar(1, SUAVE, 3);

        assertThat(existencias(SUAVE)).isEqualTo(3);
        assertThat(usuario.getMonedas()).isEqualTo(55);
    }

    @Test
    void comprarSinMonedasDa402YNoSuma() {
        usuario.setMonedas(10);
        falla(HttpStatus.PAYMENT_REQUIRED, () -> service.comprar(1, SUAVE, null));
        assertThat(existencias(SUAVE)).isZero();
        assertThat(usuario.getMonedas()).isEqualTo(10);
    }

    @Test
    void comprarUnoQueNoEsAbonoOInexistenteDa404() {
        ItemTienda maceta = abono(9, "MACETA_X", 10, 2);
        maceta.setTipo("MACETA");
        falla(HttpStatus.NOT_FOUND, () -> service.comprar(1, 9, null));
        when(itemRepo.findById(99)).thenReturn(Optional.empty());
        falla(HttpStatus.NOT_FOUND, () -> service.comprar(1, 99, null));
        assertThat(usuario.getMonedas()).isEqualTo(100);
    }

    @Test
    void comprarUnAbonoDesactivadoDa404() {
        itemRepo.findById(SUAVE).get().setActivo(false);
        falla(HttpStatus.NOT_FOUND, () -> service.comprar(1, SUAVE, null));
    }

    @Test
    void cantidadFueraDeRangoDa400() {
        falla(HttpStatus.BAD_REQUEST, () -> service.comprar(1, SUAVE, 0));
        falla(HttpStatus.BAD_REQUEST, () -> service.comprar(1, SUAVE, -2));
        falla(HttpStatus.BAD_REQUEST, () -> service.comprar(1, SUAVE, AbonoService.MAX_POR_COMPRA + 1));
        assertThat(usuario.getMonedas()).isEqualTo(100);
    }

    @Test
    void noPasaDeLasExistenciasMaximas() {
        stock.put(SUAVE, AbonoService.MAX_EXISTENCIAS);
        falla(HttpStatus.CONFLICT, () -> service.comprar(1, SUAVE, null));
        assertThat(usuario.getMonedas()).isEqualTo(100);
    }

    @Test
    void faltaDeNivelDa409() {
        itemRepo.findById(REFORZADO).get().setNivelRequerido(3);
        usuario.setNivel(1);
        falla(HttpStatus.CONFLICT, () -> service.comprar(1, REFORZADO, null));
        assertThat(existencias(REFORZADO)).isZero();
    }

    // ---- inventario ----

    @Test
    void inventarioListaCadaAbonoConSuContador() {
        listarItems();
        stock.put(ORGANICO, 2);

        AbonosDto dto = service.inventario(1);

        assertThat(dto.monedas()).isEqualTo(100);
        assertThat(dto.abonos()).extracting(AbonosDto.Abono::codigo)
                .containsExactly("ABONO_MEDIO", "ABONO_1", "ABONO_3");
        assertThat(dto.abonos()).extracting(AbonosDto.Abono::cantidad).containsExactly(0, 2, 0);
        assertThat(dto.abonos()).extracting(AbonosDto.Abono::mediosDias).containsExactly(1, 2, 6);
    }

    @Test
    void inventarioOcultaLosDesactivadosSinExistenciasPeroMuestraLosQueAunTiene() {
        listarItems();
        itemRepo.findById(SUAVE).get().setActivo(false);
        itemRepo.findById(REFORZADO).get().setActivo(false);
        stock.put(REFORZADO, 1);

        assertThat(service.inventario(1).abonos()).extracting(AbonosDto.Abono::codigo)
                .containsExactly("ABONO_1", "ABONO_3");
    }

    // ---- usar ----

    @Test
    void usarDescuentaUnoYSumaUnDia() {
        stock.put(ORGANICO, 2);

        service.usar(1, 10, ORGANICO, HOY);

        assertThat(existencias(ORGANICO)).isEqualTo(1);
        assertThat(usuario.getMonedas()).isEqualTo(100); // usar no cobra
        assertThat(planta.getDiasCuidados()).isEqualTo(1);
        assertThat(planta.getUltimoDiaAbonoTienda()).isEqualTo(HOY);
        verify(plantaRepo).save(planta);
    }

    @Test
    void sinExistenciasDa409YNoCambiaNada() {
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));
        assertThat(planta.getDiasCuidados()).isZero();
        assertThat(planta.getUltimoDiaAbonoTienda()).isNull();
        verify(usuarioRepo, never()).save(org.mockito.ArgumentMatchers.any());
        verify(plantaRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void maximoUnAbonoAlDiaPorPlantaYNoDescuentaElRechazado() {
        stock.put(ORGANICO, 3);
        stock.put(SUAVE, 1);
        service.usar(1, 10, ORGANICO, HOY);
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, SUAVE, HOY));
        assertThat(existencias(ORGANICO)).isEqualTo(2);
        assertThat(existencias(SUAVE)).isEqualTo(1);

        service.usar(1, 10, ORGANICO, HOY.plusDays(1));
        assertThat(planta.getDiasCuidados()).isEqualTo(2);
        assertThat(existencias(ORGANICO)).isEqualTo(1);
    }

    @Test
    void abonoSuaveGuardaMedioDiaYElSiguienteLoCompleta() {
        stock.put(SUAVE, 2);
        service.usar(1, 10, SUAVE, HOY);
        assertThat(planta.getDiasCuidados()).isZero();
        assertThat(planta.isMedioDiaPendiente()).isTrue();

        service.usar(1, 10, SUAVE, HOY.plusDays(1));
        assertThat(planta.getDiasCuidados()).isEqualTo(1);
        assertThat(planta.isMedioDiaPendiente()).isFalse();
    }

    @Test
    void abonoReforzadoSumaTresDias() {
        stock.put(REFORZADO, 1);
        service.usar(1, 10, REFORZADO, HOY);
        assertThat(planta.getDiasCuidados()).isEqualTo(3);
        assertThat(existencias(REFORZADO)).isZero();
    }

    @Test
    void sinIdItemDa400() {
        falla(HttpStatus.BAD_REQUEST, () -> service.usar(1, 10, null, HOY));
    }

    @Test
    void unItemQueNoEsAbonoDa404() {
        ItemTienda maceta = abono(8, "MACETA_X", 10, 2);
        maceta.setTipo("MACETA");
        falla(HttpStatus.NOT_FOUND, () -> service.usar(1, 10, 8, HOY));
    }

    @Test
    void siSeDesactivoEnLaTiendaSeSigueUsandoSiYaLoTenia() {
        itemRepo.findById(ORGANICO).get().setActivo(false);
        stock.put(ORGANICO, 1);
        service.usar(1, 10, ORGANICO, HOY);
        assertThat(planta.getDiasCuidados()).isEqualTo(1);
    }

    @Test
    void plantaAjenaDa404YNoDescuenta() {
        stock.put(ORGANICO, 1);
        planta.setIdUsuario(2);
        falla(HttpStatus.NOT_FOUND, () -> service.usar(1, 10, ORGANICO, HOY));
        assertThat(existencias(ORGANICO)).isEqualTo(1);
    }

    @Test
    void plantaMarchitaOMuertaDa409YNoDescuenta() {
        stock.put(ORGANICO, 1);
        planta.setSaludPorcentaje(20);
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));
        planta.setSaludPorcentaje(80);
        planta.setEtapaCrecimiento("MUERTA");
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));
        assertThat(existencias(ORGANICO)).isEqualTo(1);
    }

    @Test
    void plantaQueYaCrecioDelTodoDa409YNoDescuenta() {
        stock.put(ORGANICO, 1);
        planta.setDiasCuidados(7); // sin forma: 7 dias
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));
        assertThat(existencias(ORGANICO)).isEqualTo(1);
    }

    @Test
    void plantaEnElBaulDa409YNoDescuenta() {
        stock.put(ORGANICO, 1);
        planta.setEnBaul(true);
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));
        assertThat(existencias(ORGANICO)).isEqualTo(1);
    }

    @Test
    void elAbonoUsadoCuentaParaElLogroDeAbonosYElRechazadoNo() {
        stock.put(ORGANICO, 2);
        service.usar(1, 10, ORGANICO, HOY);
        falla(HttpStatus.CONFLICT, () -> service.usar(1, 10, ORGANICO, HOY));

        assertThat(usuario.getAbonosTotal()).isEqualTo(1);
    }

    @Test
    void comprarNoCuentaParaElLogroDeAbonos() {
        service.comprar(1, ORGANICO, 2);
        assertThat(usuario.getAbonosTotal()).isZero();
    }
}
