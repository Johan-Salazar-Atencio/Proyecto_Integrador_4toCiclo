package com.plantia.services;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.models.ItemTienda;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.Usuario;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.UsuarioRepository;

/**
 * Compra y uso de un abono de la tienda: cuesta monedas y suma medio dia, uno o tres dias de crecimiento segun el
 * abono (maximo un abono al dia por planta). Sin elegir abono se usa el mas barato.
 */
@Service
public class AbonoService {

    private final PlantaUsuarioRepository plantaRepo;
    private final ItemTiendaRepository itemRepo;
    private final UsuarioRepository usuarioRepo;
    private final PlantasService plantas;

    public AbonoService(PlantaUsuarioRepository plantaRepo, ItemTiendaRepository itemRepo,
                        UsuarioRepository usuarioRepo, PlantasService plantas) {
        this.plantaRepo = plantaRepo;
        this.itemRepo = itemRepo;
        this.usuarioRepo = usuarioRepo;
        this.plantas = plantas;
    }

    @Transactional
    public void abonar(Integer userId, Integer idPlantaUsuario, Integer idItem) {
        abonar(userId, idPlantaUsuario, idItem, LocalDate.now());
    }

    void abonar(Integer userId, Integer idPlantaUsuario, Integer idItem, LocalDate hoy) {
        usuarioRepo.bloquear(userId); // antes de leer saldo y "abono de hoy": evita pagar o abonar dos veces
        PlantaUsuario p = plantaRepo.findById(idPlantaUsuario)
                .filter(x -> userId.equals(x.getIdUsuario()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planta no encontrada"));
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
        ItemTienda abono = (idItem == null
                ? itemRepo.findFirstByTipoAndActivoTrueOrderByPrecioMonedasAsc("ABONO")
                : itemRepo.findById(idItem)
                        .filter(i -> "ABONO".equals(i.getTipo()) && Boolean.TRUE.equals(i.getActivo())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese abono no esta en la tienda"));
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        int precio = valor(abono.getPrecioMonedas());
        if (valor(u.getMonedas()) < precio) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "No te alcanzan las monedas");
        }
        u.setMonedas(valor(u.getMonedas()) - precio);
        p.setUltimoDiaAbonoTienda(hoy);
        PlantasService.sumarMediosDias(p, valor(abono.getMediosDias()), total);
        usuarioRepo.save(u);
        plantaRepo.save(p);
    }

    private static int valor(Integer n) {
        return n != null ? n : 0;
    }
}
