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

/** La tienda vende cada fondo una sola vez, respeta nivel y monedas, y equipa uno por momento del dia. */
class TiendaServiceTest {

    private static final int USER = 1;

    private ItemTiendaRepository itemRepo;
    private InventarioUsuarioRepository inventarioRepo;
    private FondoEscenaRepository fondoRepo;
    private UsuarioRepository usuarioRepo;
    private TiendaService service;
    private Usuario usuario;
    private final List<InventarioUsuario> inventario = new ArrayList<>();

    private static ItemTienda item(int id, String tipo, int precio, int nivel, Integer idFondo, boolean activo) {
        ItemTienda i = new ItemTienda();
        i.setIdItem(id);
        i.setNombre("Item " + id);
        i.setTipo(tipo);
        i.setPrecioMonedas(precio);
        i.setNivelRequerido(nivel);
        i.setIdFondo(idFondo);
        i.setActivo(activo);
        i.setAssetUrl("/files/arte/fondos/x_vista.png");
        return i;
    }

    private static FondoEscena fondo(int id, String momento) {
        FondoEscena f = new FondoEscena();
        ReflectionTestUtils.setField(f, "idFondo", id);
        f.setMomento(momento);
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
        ItemTienda nevado = item(2, "FONDO", 100, 1, 20, true);     // DIA
        ItemTienda sakura = item(3, "FONDO", 180, 2, 21, true);     // DIA
        ItemTienda cyber = item(4, "FONDO", 250, 3, 22, true);      // NOCHE, falta nivel
        ItemTienda viejo = item(5, "FONDO", 50, 1, 23, false);      // desactivado
        for (ItemTienda i : List.of(abono, nevado, sakura, cyber, viejo)) {
            when(itemRepo.findById(i.getIdItem())).thenReturn(Optional.of(i));
        }
        when(itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc()).thenReturn(List.of(abono, nevado, sakura, cyber));
        when(itemRepo.findAllById(any())).thenAnswer(inv -> {
            List<Integer> ids = new ArrayList<>();
            ((Iterable<Integer>) inv.getArgument(0)).forEach(ids::add);
            return List.of(abono, nevado, sakura, cyber, viejo).stream().filter(i -> ids.contains(i.getIdItem())).toList();
        });
        when(fondoRepo.findAllById(any())).thenAnswer(inv -> {
            List<Integer> ids = new ArrayList<>();
            ((Iterable<Integer>) inv.getArgument(0)).forEach(ids::add);
            return List.of(fondo(20, "DIA"), fondo(21, "DIA"), fondo(22, "NOCHE"), fondo(23, "TARDE")).stream()
                    .filter(f -> ids.contains(f.getIdFondo())).toList();
        });
        when(fondoRepo.findById(20)).thenReturn(Optional.of(fondo(20, "DIA")));
        when(fondoRepo.findById(21)).thenReturn(Optional.of(fondo(21, "DIA")));
        when(inventarioRepo.findByIdUsuario(USER)).thenAnswer(i -> new ArrayList<>(inventario));
        when(inventarioRepo.save(any())).thenAnswer(i -> {
            InventarioUsuario inv = i.getArgument(0);
            if (inv.getIdInventario() == null) {
                ReflectionTestUtils.setField(inv, "idInventario", inventario.size() + 100);
                inventario.add(inv);
            }
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
        assertThat(t.items()).extracting(TiendaDto.Item::id).containsExactly(1, 2, 3, 4);
        assertThat(en(t, 4).bloqueado()).isTrue();
        assertThat(en(t, 3).bloqueado()).isFalse();
        assertThat(en(t, 2).momento()).isEqualTo("DIA");
        assertThat(en(t, 1).comprado()).isFalse();
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
    void elAbonoNoSeCompraAqui() {
        assertThatThrownBy(() -> service.comprar(USER, 1))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(usuario.getMonedas()).isEqualTo(300);
    }

    @Test
    void equiparUnFondoQuitaElOtroDelMismoMomento() {
        service.comprar(USER, 2);
        service.comprar(USER, 3);

        TiendaDto t = service.equipar(USER, 2);
        assertThat(en(t, 2).equipado()).isTrue();

        t = service.equipar(USER, 3);
        assertThat(en(t, 3).equipado()).isTrue();
        assertThat(en(t, 2).equipado()).isFalse();
    }

    @Test
    void sinComprarNoSePuedeEquiparYDesequiparVuelveAlPredeterminado() {
        assertThatThrownBy(() -> service.equipar(USER, 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        service.comprar(USER, 2);
        service.equipar(USER, 2);
        TiendaDto t = service.desequipar(USER, 2);
        assertThat(en(t, 2).equipado()).isFalse();
        assertThat(en(t, 2).comprado()).isTrue();
    }

    @Test
    void losFondosEquipadosSeUsanParaLaEscena() {
        service.comprar(USER, 2);
        service.equipar(USER, 2);
        when(inventarioRepo.findByIdUsuarioAndEstaEquipadoTrue(USER))
                .thenReturn(inventario.stream().filter(i -> Boolean.TRUE.equals(i.getEstaEquipado())).toList());

        assertThat(service.fondosEquipados(USER)).containsExactly(20);
    }
}
