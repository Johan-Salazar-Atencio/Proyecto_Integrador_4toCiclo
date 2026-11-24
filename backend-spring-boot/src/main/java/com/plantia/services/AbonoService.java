package com.plantia.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Abonos como inventario: la tienda los vende (cuesta monedas y suma al contador del usuario) y la planta los usa
 * (descuenta 1 y suma medio dia, uno o tres dias de crecimiento segun el abono; maximo un abono al dia por planta).
 */
@Service
public class AbonoService {

    /** Existencias maximas por tipo de abono y compra maxima de una vez. */
    static final int MAX_EXISTENCIAS = 99;
    static final int MAX_POR_COMPRA = 10;

    private final PlantaUsuarioRepository plantaRepo;
    private final ItemTiendaRepository itemRepo;
    private final UsuarioRepository usuarioRepo;
    private final PlantasService plantas;
    private final AbonoUsuarioRepository abonoRepo;

    public AbonoService(PlantaUsuarioRepository plantaRepo, ItemTiendaRepository itemRepo,
                        UsuarioRepository usuarioRepo, PlantasService plantas, AbonoUsuarioRepository abonoRepo) {
        this.plantaRepo = plantaRepo;
        this.itemRepo = itemRepo;
        this.usuarioRepo = usuarioRepo;
        this.plantas = plantas;
        this.abonoRepo = abonoRepo;
    }

    /** Logros (ABONOS_TOTAL) al momento, como tras un cuidado; antes se celebraban recien en la siguiente accion (QA-16). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.beans.factory.ObjectProvider<LogroService> logros;

    /** True si el articulo existe, esta activo y es un abono (la tienda decide con esto como comprarlo). */
    public boolean esAbono(Integer idItem) {
        return idItem != null && itemRepo.findById(idItem)
                .filter(i -> "ABONO".equals(i.getTipo()) && Boolean.TRUE.equals(i.getActivo())).isPresent();
    }

    /** Inventario del usuario: monedas y cuantos abonos de cada tipo tiene. */
    @Transactional(readOnly = true)
    public AbonosDto inventario(Integer userId) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Map<Integer, Integer> existencias = existencias(userId);
        List<AbonosDto.Abono> abonos = itemRepo.findByTipoOrderByPrecioMonedasAscIdItemAsc("ABONO").stream()
                .filter(i -> Boolean.TRUE.equals(i.getActivo()) || existencias.getOrDefault(i.getIdItem(), 0) > 0)
                .map(i -> new AbonosDto.Abono(i.getIdItem(), i.getCodigo(), i.getNombre(),
                        i.getDescripcion() != null ? i.getDescripcion() : "", valor(i.getPrecioMonedas()),
                        valor(i.getMediosDias()), i.getAssetUrl(), existencias.getOrDefault(i.getIdItem(), 0)))
                .toList();
        return new AbonosDto(valor(u.getMonedas()), abonos);
    }

    /** Existencias por articulo (solo los que tiene), para la tienda y el inventario. */
    public Map<Integer, Integer> existencias(Integer userId) {
        Map<Integer, Integer> mapa = new HashMap<>();
        for (AbonoUsuario a : abonoRepo.findByIdUsuario(userId)) {
            mapa.put(a.getIdItem(), valor(a.getCantidad()));
        }
        return mapa;
    }

    /**
     * Compra {@code cantidad} abonos del tipo (1 a {@value #MAX_POR_COMPRA}) y los suma al contador, sin usarlos.
     * 404 si no es un abono de la tienda; 400 si la cantidad no vale; 409 si falta nivel o pasaria de
     * {@value #MAX_EXISTENCIAS}; 402 sin monedas.
     */
    @Transactional
    public AbonosDto comprar(Integer userId, Integer idItem, Integer cantidad) {
        int n = cantidad != null ? cantidad : 1;
        if (n < 1 || n > MAX_POR_COMPRA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Puedes comprar de 1 a " + MAX_POR_COMPRA + " abonos a la vez");
        }
        usuarioRepo.bloquear(userId); // antes de leer saldo y existencias: evita el doble gasto
        ItemTienda abono = itemRepo.findById(idItem)
                .filter(i -> "ABONO".equals(i.getTipo()) && Boolean.TRUE.equals(i.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese abono no está en la tienda"));
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (valor(abono.getNivelRequerido()) > Math.max(1, valor(u.getNivel()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Necesitas el nivel " + abono.getNivelRequerido());
        }
        if (existencias(userId).getOrDefault(idItem, 0) + n > MAX_EXISTENCIAS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puedes guardar más de " + MAX_EXISTENCIAS + " abonos de este tipo");
        }
        int total = valor(abono.getPrecioMonedas()) * n;
        if (valor(u.getMonedas()) < total) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "No te alcanzan las monedas");
        }
        u.setMonedas(valor(u.getMonedas()) - total);
        usuarioRepo.save(u);
        abonoRepo.sumar(userId, idItem, n);
        return inventario(userId);
    }

    /** Usa un abono del inventario en la planta (descuenta 1). Ver {@link #usar(Integer, Integer, Integer, LocalDate)}. */
    @Transactional
    public void usar(Integer userId, Integer idPlantaUsuario, Integer idItem) {
        usar(userId, idPlantaUsuario, idItem, LocalDate.now());
        LogroService servicio = logros != null ? logros.getIfAvailable() : null;
        if (servicio != null) {
            servicio.evaluarSeguro(userId, java.time.LocalDateTime.now()); // tras el commit y sin tumbar el abono
        }
    }

    /**
     * Descuenta 1 abono del tipo y suma sus dias de crecimiento a la planta. 400 sin idItem; 404 si la planta no es
     * suya o el articulo no es un abono; 409 si esta en el baul, muerta o marchita, ya crecio del todo, ya uso un
     * abono hoy o no le quedan de ese tipo (nada se descuenta en ningun rechazo).
     */
    void usar(Integer userId, Integer idPlantaUsuario, Integer idItem, LocalDate hoy) {
        if (idItem == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta idItem: elige qué abono usar");
        }
        usuarioRepo.bloquear(userId); // antes de leer existencias y "abono de hoy": evita usar dos veces el mismo
        plantaRepo.bloquear(idPlantaUsuario); // QA-03: un GET simultaneo no pisa los dias que suma el abono
        PlantaUsuario p = plantaRepo.findById(idPlantaUsuario)
                .filter(x -> userId.equals(x.getIdUsuario()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planta no encontrada"));
        if (PlantasService.enBaul(p)) {
            // QA-16: la planta del baul esta congelada
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planta está en el baúl: sácala primero");
        }
        int total = CrecimientoPlanta.diasTotales(plantas.apariencia(p));
        if ("MUERTA".equals(p.getEtapaCrecimiento())
                || (p.getSaludPorcentaje() != null && p.getSaludPorcentaje() < PlantasService.SALUD_MARCHITA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primero hay que recuperar la planta");
        }
        if (valor(p.getDiasCuidados()) >= total) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planta ya creció del todo");
        }
        if (hoy.equals(p.getUltimoDiaAbonoTienda())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya usaste un abono con esta planta hoy");
        }
        // Un abono desactivado en la tienda se sigue pudiendo usar si el usuario ya lo tenia
        ItemTienda abono = itemRepo.findById(idItem)
                .filter(i -> "ABONO".equals(i.getTipo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese abono no existe"));
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (abonoRepo.descontarUno(userId, idItem) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No te quedan abonos de este tipo: compra uno en la tienda");
        }
        // Un abono usado tambien es un abono real para el logro ABONOS_TOTAL (ya esta bloqueado arriba)
        ProgresoService.sumarCuidado(u, "ABONAR");
        p.setUltimoDiaAbonoTienda(hoy);
        PlantasService.sumarMediosDias(p, valor(abono.getMediosDias()), total);
        usuarioRepo.save(u);
        plantaRepo.save(p);
    }

    private static int valor(Integer n) {
        return n != null ? n : 0;
    }
}
