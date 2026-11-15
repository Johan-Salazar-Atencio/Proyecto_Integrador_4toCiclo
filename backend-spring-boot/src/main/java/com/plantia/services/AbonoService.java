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

/** Compra y uso del abono de la tienda: cuesta monedas y suma un dia de crecimiento (maximo uno al dia por planta). */
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
    public void abonar(Integer userId, Integer idPlantaUsuario) {
        abonar(userId, idPlantaUsuario, LocalDate.now());
    }

    void abonar(Integer userId, Integer idPlantaUsuario, LocalDate hoy) {
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
        ItemTienda abono = itemRepo.findFirstByTipoAndActivoTrueOrderByPrecioMonedasAsc("ABONO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La tienda no tiene abono"));
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        int precio = valor(abono.getPrecioMonedas());
        if (valor(u.getMonedas()) < precio) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "No te alcanzan las monedas");
        }
        u.setMonedas(valor(u.getMonedas()) - precio);
        p.setUltimoDiaAbonoTienda(hoy);
        PlantasService.sumarDia(p, total);
        usuarioRepo.save(u);
        plantaRepo.save(p);
    }

    private static int valor(Integer n) {
        return n != null ? n : 0;
    }
}
