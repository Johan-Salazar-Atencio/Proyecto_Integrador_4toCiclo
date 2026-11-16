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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.TiendaDto;
import com.plantia.models.FondoEscena;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.Usuario;
import com.plantia.repositories.FondoEscenaRepository;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.UsuarioRepository;

/** La tienda vende cada tema de fondo una sola vez y respeta nivel y monedas. */
class TiendaServiceTest {

    private static final int USER = 1;

    private ItemTiendaRepository itemRepo;
    private InventarioUsuarioRepository inventarioRepo;
    private FondoEscenaRepository fondoRepo;
    private UsuarioRepository usuarioRepo;
    private TiendaService service;
    private Usuario usuario;
    private final List<InventarioUsuario> inventario = new ArrayList<>();

    private static ItemTienda item(int id, String tipo, int precio, int nivel, Integer idTema, boolean activo) {
        ItemTienda i = new ItemTienda();
        i.setIdItem(id);
        i.setNombre("Item " + id);
        i.setTipo(tipo);
        i.setPrecioMonedas(precio);
        i.setNivelRequerido(nivel);
        i.setIdTema(idTema);
        i.setActivo(activo);
        i.setAssetUrl("/files/arte/fondos/x_vista.png");
        return i;
    }

    private static FondoEscena escena(int id, int idTema, String momento) {
        FondoEscena f = new FondoEscena();
        f.setIdFondo(id);
        f.setIdTema(idTema);
        f.setMomento(momento);
        f.setImagenUrl("/files/arte/fondos/t" + idTema + "_" + momento + ".png");
        f.setVistaUrl("/files/arte/fondos/t" + idTema + "_" + momento + "_vista.png");
        return f;
    }

    @BeforeEach
    void preparar() {
        itemRepo = mock(ItemTiendaRepository.class);
        inventarioRepo = mock(InventarioUsuarioRepository.class);
        fondoRepo = mock(FondoEscenaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        service = new TiendaService(itemRepo, inventarioRepo, fondoRepo, usuarioRepo);

        usuario = new Usuario();
        usuario.setMonedas(300);
        usuario.setNivel(2);
        when(usuarioRepo.findById(USER)).thenReturn(Optional.of(usuario));

        ItemTienda abono = item(1, "ABONO", 30, 1, null, true);
        ItemTienda nevado = item(2, "FONDO", 100, 1, 20, true);
        ItemTienda cyber = item(4, "FONDO", 250, 3, 22, true);      // falta nivel
        ItemTienda viejo = item(5, "FONDO", 50, 1, 23, false);      // desactivado
        ItemTienda sinTema = item(6, "FONDO", 50, 1, null, true);   // fondo suelto de antes: aun sin tema
        for (ItemTienda i : List.of(abono, nevado, cyber, viejo, sinTema)) {
            when(itemRepo.findById(i.getIdItem())).thenReturn(Optional.of(i));
        }
        when(itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc()).thenReturn(List.of(abono, nevado, cyber, sinTema));
        // El tema 20 trae sus tres escenas desordenadas; el 22 solo la noche
        when(fondoRepo.findByIdTemaInAndActivoTrue(any())).thenReturn(List.of(
                escena(1, 20, "NOCHE"), escena(2, 20, "DIA"), escena(3, 20, "TARDE"), escena(4, 22, "NOCHE")));
        when(inventarioRepo.findByIdUsuario(USER)).thenAnswer(i -> new ArrayList<>(inventario));
        when(inventarioRepo.save(any())).thenAnswer(i -> {
            InventarioUsuario inv = i.getArgument(0);
            inventario.add(inv);
            return inv;
        });
    }

    private TiendaDto.Item en(TiendaDto t, int id) {
        return t.items().stream().filter(i -> i.id() == id).findFirst().orElseThrow();
    }

    @Test
    void listaSoloLoActivoYMarcaElNivelQueFalta() {
        TiendaDto t = service.listar(USER);

        assertThat(t.monedas()).isEqualTo(300);
        assertThat(t.items()).extracting(TiendaDto.Item::id).containsExactly(1, 2, 4, 6);
        assertThat(en(t, 4).bloqueado()).isTrue();
        assertThat(en(t, 2).bloqueado()).isFalse();
        assertThat(en(t, 1).comprado()).isFalse();
    }

    @Test
    void cadaFondoTraeSusEscenasEnOrdenDiaTardeNoche() {
        TiendaDto t = service.listar(USER);

        assertThat(en(t, 2).variantes()).extracting(v -> v.momento()).containsExactly("DIA", "TARDE", "NOCHE");
        assertThat(en(t, 2).variantes().get(0).vistaUrl()).endsWith("t20_DIA_vista.png");
        assertThat(en(t, 4).variantes()).extracting(v -> v.momento()).containsExactly("NOCHE");
        assertThat(en(t, 1).variantes()).isEmpty();
    }

    @Test
    void comprarUnFondoCobraYLoGuarda() {
        TiendaDto t = service.comprar(USER, 2);

        assertThat(usuario.getMonedas()).isEqualTo(200);
        assertThat(en(t, 2).comprado()).isTrue();
        assertThat(t.monedas()).isEqualTo(200);
        verify(usuarioRepo).save(usuario);
    }

    @Test
    void noSePuedeComprarDosVecesElMismoFondo() {
        service.comprar(USER, 2);
        assertThatThrownBy(() -> service.comprar(USER, 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(usuario.getMonedas()).isEqualTo(200);
    }

    @Test
    void sinMonedasDa402ConNivelDa409YUnArticuloDesactivadoDa404() {
        usuario.setMonedas(10);
        assertThatThrownBy(() -> service.comprar(USER, 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));
        usuario.setMonedas(1000);
        assertThatThrownBy(() -> service.comprar(USER, 4))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.comprar(USER, 5))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(inventarioRepo, never()).save(any());
    }

    @Test
    void elAbonoYUnFondoSinTemaNoSeCompranAqui() {
        for (int id : new int[] {1, 6}) {
            assertThatThrownBy(() -> service.comprar(USER, id))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }
        assertThat(usuario.getMonedas()).isEqualTo(300);
    }

    // --- Macetas e invernaderos ---

    private ItemTienda articulo(int id, String tipo, int precio, String codigo) {
        ItemTienda i = item(id, tipo, precio, 1, null, true);
        i.setCodigo(codigo);
        when(itemRepo.findById(id)).thenReturn(Optional.of(i));
        return i;
    }

    @Test
    void lasMacetasYLosInvernaderosGratisYaSonDelUsuarioYLosDemasNo() {
        ItemTienda terracota = articulo(10, "MACETA", 0, "TERRACOTA");
        ItemTienda crema = articulo(11, "MACETA", 80, "CREMA");
        ItemTienda selva = articulo(12, "INVERNADERO", 0, "SELVA");
        when(itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc()).thenReturn(List.of(terracota, crema, selva));

        TiendaDto t = service.listar(USER);

        assertThat(en(t, 10).comprado()).isTrue();
        assertThat(en(t, 11).comprado()).isFalse();
        assertThat(en(t, 12).comprado()).isTrue();
        assertThat(en(t, 11).codigo()).isEqualTo("CREMA");
    }

    @Test
    void comprarUnaMacetaCobraYLaGuarda() {
        ItemTienda crema = articulo(11, "MACETA", 80, "CREMA");
        when(itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc()).thenReturn(List.of(crema));

        TiendaDto t = service.comprar(USER, 11);

        assertThat(usuario.getMonedas()).isEqualTo(220);
        assertThat(en(t, 11).comprado()).isTrue();
    }

    @Test
    void comprarUnInvernaderoTambienSePuede() {
        ItemTienda victoriano = articulo(13, "INVERNADERO", 150, "VICTORIANO");
        when(itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc()).thenReturn(List.of(victoriano));

        service.comprar(USER, 13);

        assertThat(usuario.getMonedas()).isEqualTo(150);
    }

    @Test
    void unaMacetaGratisNoSeCompra() {
        articulo(10, "MACETA", 0, "TERRACOTA");

        assertThatThrownBy(() -> service.comprar(USER, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(usuario.getMonedas()).isEqualTo(300);
    }

    @Test
    void sinMonedasNoSeCompraUnaMaceta() {
        usuario.setMonedas(50);
        articulo(11, "MACETA", 80, "CREMA");

        assertThatThrownBy(() -> service.comprar(USER, 11))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));
    }
}
