package com.plantia.services;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.TemaFondoDto;
import com.plantia.dto.TiendaDto;
import com.plantia.models.FondoEscena;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.Usuario;
import com.plantia.repositories.FondoEscenaRepository;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.UsuarioRepository;

/**
 * Tienda: lista lo que el admin tiene activo y vende temas de fondo (una sola vez por usuario). Poner un
 * fondo a una planta se hace desde el jardin (FondosPlantaService). El abono no pasa por el inventario:
 * se compra y se usa en el momento desde la planta (AbonoService).
 */
@Service
public class TiendaService {

    static final String FONDO = "FONDO";
    private static final List<String> ORDEN_MOMENTOS = List.of("DIA", "TARDE", "NOCHE");

    private final ItemTiendaRepository itemRepo;
    private final InventarioUsuarioRepository inventarioRepo;
    private final FondoEscenaRepository fondoRepo;
    private final UsuarioRepository usuarioRepo;

    public TiendaService(ItemTiendaRepository itemRepo, InventarioUsuarioRepository inventarioRepo,
                         FondoEscenaRepository fondoRepo, UsuarioRepository usuarioRepo) {
        this.itemRepo = itemRepo;
        this.inventarioRepo = inventarioRepo;
        this.fondoRepo = fondoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional(readOnly = true)
    public TiendaDto listar(Integer userId) {
        Usuario u = usuario(userId);
        List<Integer> comprados = inventarioRepo.findByIdUsuario(userId).stream().map(InventarioUsuario::getIdItem).toList();
        List<ItemTienda> items = itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc().stream()
                .filter(i -> !"MACETA".equals(i.getTipo()))
                .toList();
        Map<Integer, List<TemaFondoDto.Variante>> variantes = variantesDe(
                items.stream().map(ItemTienda::getIdTema).filter(Objects::nonNull).distinct().toList());
        int nivel = valor(u.getNivel(), 1);
        return new TiendaDto(valor(u.getMonedas(), 0), nivel, items.stream()
                .map(i -> new TiendaDto.Item(i.getIdItem(), i.getNombre(), i.getTipo(),
                        i.getDescripcion() != null ? i.getDescripcion() : "",
                        valor(i.getPrecioMonedas(), 0), valor(i.getNivelRequerido(), 1), i.getAssetUrl(),
                        i.getIdTema() != null ? variantes.getOrDefault(i.getIdTema(), List.of()) : List.of(),
                        comprados.contains(i.getIdItem()), valor(i.getNivelRequerido(), 1) > nivel))
                .toList());
    }

    /** Compra un tema de fondo. 404 si no existe o esta desactivado; 409 si ya lo tiene, falta nivel o no es un fondo; 402 sin monedas. */
    @Transactional
    public TiendaDto comprar(Integer userId, Integer idItem) {
        ItemTienda item = itemRepo.findById(idItem)
                .filter(i -> Boolean.TRUE.equals(i.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));
        if (!FONDO.equals(item.getTipo()) || item.getIdTema() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ABONO".equals(item.getTipo()) ? "El abono se usa desde la planta del jardin" : "Este articulo aun no esta a la venta");
        }
        Usuario u = usuario(userId);
        if (inventarioRepo.findByIdUsuario(userId).stream().anyMatch(i -> idItem.equals(i.getIdItem()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes este fondo");
        }
        if (valor(item.getNivelRequerido(), 1) > valor(u.getNivel(), 1)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Necesitas el nivel " + item.getNivelRequerido());
        }
        int precio = valor(item.getPrecioMonedas(), 0);
        if (valor(u.getMonedas(), 0) < precio) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "No te alcanzan las monedas");
        }
        u.setMonedas(valor(u.getMonedas(), 0) - precio);
        usuarioRepo.save(u);
        InventarioUsuario compra = new InventarioUsuario();
        compra.setIdUsuario(userId);
        compra.setIdItem(idItem);
        inventarioRepo.save(compra);
        return listar(userId);
    }

    /** Escenas de cada tema, en orden DIA, TARDE, NOCHE, con su vista previa. */
    Map<Integer, List<TemaFondoDto.Variante>> variantesDe(List<Integer> idsTema) {
        if (idsTema.isEmpty()) {
            return Map.of();
        }
        return fondoRepo.findByIdTemaInAndActivoTrue(idsTema).stream()
                .sorted(Comparator.comparingInt((FondoEscena f) -> ORDEN_MOMENTOS.indexOf(f.getMomento())))
                .collect(Collectors.groupingBy(FondoEscena::getIdTema,
                        Collectors.mapping(f -> new TemaFondoDto.Variante(f.getMomento(),
                                f.getVistaUrl() != null ? f.getVistaUrl() : f.getImagenUrl()), Collectors.toList())));
    }

    private Usuario usuario(Integer userId) {
        return usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private static int valor(Integer n, int defecto) {
        return n != null ? n : defecto;
    }
}
