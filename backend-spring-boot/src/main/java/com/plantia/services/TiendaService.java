package com.plantia.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Tienda: lista lo que el admin tiene activo, vende fondos (una sola vez por usuario) y deja equiparlos.
 * El abono no pasa por el inventario: se compra y se usa en el momento desde la planta (AbonoService).
 */
@Service
public class TiendaService {

    static final String FONDO = "FONDO";

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
        Map<Integer, InventarioUsuario> inventario = inventarioRepo.findByIdUsuario(userId).stream()
                .collect(Collectors.toMap(InventarioUsuario::getIdItem, i -> i, (a, b) -> a));
        List<ItemTienda> items = itemRepo.findByActivoTrueOrderByTipoAscPrecioMonedasAscIdItemAsc();
        Map<Integer, FondoEscena> fondos = fondosDe(items);
        int nivel = valor(u.getNivel(), 1);
        return new TiendaDto(valor(u.getMonedas(), 0), nivel, items.stream()
                .filter(i -> !"MACETA".equals(i.getTipo()))
                .map(i -> {
                    InventarioUsuario inv = inventario.get(i.getIdItem());
                    FondoEscena fondo = i.getIdFondo() != null ? fondos.get(i.getIdFondo()) : null;
                    return new TiendaDto.Item(i.getIdItem(), i.getNombre(), i.getTipo(),
                            i.getDescripcion() != null ? i.getDescripcion() : "",
                            valor(i.getPrecioMonedas(), 0), valor(i.getNivelRequerido(), 1), i.getAssetUrl(),
                            fondo != null ? fondo.getMomento() : null,
                            inv != null, inv != null && Boolean.TRUE.equals(inv.getEstaEquipado()),
                            valor(i.getNivelRequerido(), 1) > nivel);
                })
                .toList());
    }

    /** Compra un fondo. 404 si no existe o esta desactivado; 409 si ya lo tiene, falta nivel o no es un fondo; 402 sin monedas. */
    @Transactional
    public TiendaDto comprar(Integer userId, Integer idItem) {
        ItemTienda item = itemActivo(idItem);
        if (!FONDO.equals(item.getTipo()) || item.getIdFondo() == null) {
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

    /** Equipa un fondo comprado y quita el que tenia en el mismo momento del dia. */
    @Transactional
    public TiendaDto equipar(Integer userId, Integer idItem) {
        InventarioUsuario elegido = propio(userId, idItem);
        ItemTienda item = itemRepo.findById(idItem)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));
        FondoEscena fondo = item.getIdFondo() != null ? fondoRepo.findById(item.getIdFondo()).orElse(null) : null;
        if (fondo == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este articulo no se puede equipar");
        }
        List<InventarioUsuario> suyos = inventarioRepo.findByIdUsuario(userId);
        Map<Integer, ItemTienda> items = itemRepo.findAllById(
                suyos.stream().map(InventarioUsuario::getIdItem).toList()).stream()
                .collect(Collectors.toMap(ItemTienda::getIdItem, i -> i));
        Map<Integer, FondoEscena> fondos = fondosDe(items.values());
        for (InventarioUsuario inv : suyos) {
            ItemTienda otro = items.get(inv.getIdItem());
            FondoEscena f = otro != null && otro.getIdFondo() != null ? fondos.get(otro.getIdFondo()) : null;
            if (f != null && fondo.getMomento().equals(f.getMomento())) {
                inv.setEstaEquipado(inv.getIdInventario().equals(elegido.getIdInventario()));
            }
        }
        inventarioRepo.saveAll(suyos);
        return listar(userId);
    }

    /** Vuelve al fondo predeterminado de ese momento del dia. */
    @Transactional
    public TiendaDto desequipar(Integer userId, Integer idItem) {
        InventarioUsuario inv = propio(userId, idItem);
        inv.setEstaEquipado(false);
        inventarioRepo.save(inv);
        return listar(userId);
    }

    /** Fondos que el usuario tiene equipados (los usa ArteService para elegir el fondo de la escena). */
    @Transactional(readOnly = true)
    public Set<Integer> fondosEquipados(Integer userId) {
        List<Integer> items = inventarioRepo.findByIdUsuarioAndEstaEquipadoTrue(userId).stream()
                .map(InventarioUsuario::getIdItem).toList();
        if (items.isEmpty()) {
            return Set.of();
        }
        return itemRepo.findAllById(items).stream()
                .map(ItemTienda::getIdFondo).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }

    private Map<Integer, FondoEscena> fondosDe(java.util.Collection<ItemTienda> items) {
        List<Integer> ids = items.stream().map(ItemTienda::getIdFondo).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Integer, FondoEscena> res = new HashMap<>();
        fondoRepo.findAllById(ids).forEach(f -> res.put(f.getIdFondo(), f));
        return res;
    }

    private ItemTienda itemActivo(Integer idItem) {
        return itemRepo.findById(idItem)
                .filter(i -> Boolean.TRUE.equals(i.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));
    }

    private InventarioUsuario propio(Integer userId, Integer idItem) {
        return inventarioRepo.findByIdUsuario(userId).stream()
                .filter(i -> idItem.equals(i.getIdItem()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tienes ese articulo"));
    }

    private Usuario usuario(Integer userId) {
        return usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private static int valor(Integer n, int defecto) {
        return n != null ? n : defecto;
    }
}
