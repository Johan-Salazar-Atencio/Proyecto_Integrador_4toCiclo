package com.plantia.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.plantia.dto.PlantSummaryDto;
import com.plantia.models.EscaneoHistorial;
import com.plantia.dto.TemaFondoDto;
import com.plantia.services.AbonoService;
import com.plantia.services.FondosPlantaService;
import com.plantia.services.PlantasService;

@RestController
@RequestMapping("/api/v1/plantas")
public class PlantasController {

    private final PlantasService plantasService;
    private final AbonoService abonoService;
    private final FondosPlantaService fondosPlantaService;

    public PlantasController(PlantasService plantasService, AbonoService abonoService,
                             FondosPlantaService fondosPlantaService) {
        this.plantasService = plantasService;
        this.abonoService = abonoService;
        this.fondosPlantaService = fondosPlantaService;
    }

    /**
     * Digitaliza una planta en "Mi jardin" y la deja como activa.
     * Body: { "idEscaneo": n } o { "idCatalogo": n }, y opcional "apariencia": {...} (de la IA).
     */
    @PostMapping
    public ResponseEntity<PlantSummaryDto> crear(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer idEscaneo = entero(body.get("idEscaneo"));
        Integer idCatalogo = entero(body.get("idCatalogo"));
        if (idEscaneo == null && idCatalogo == null) {
            return ResponseEntity.badRequest().build();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> apariencia = body.get("apariencia") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        return ResponseEntity.ok(plantasService.digitalizar(userId, idEscaneo, idCatalogo, apariencia));
    }

    /** Elige la planta que se muestra en "Mi jardin". */
    @PostMapping("/{id}/seleccionar")
    public PlantSummaryDto seleccionar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return plantasService.seleccionar(userId, idPlantaUsuario);
    }

    private static Integer entero(Object valor) {
        return valor instanceof Number n ? n.intValue() : null;
    }

    /** Lista todas las plantas digitales del usuario. */
    @GetMapping
    public ResponseEntity<List<PlantSummaryDto>> listar(
            @RequestHeader("X-User-Id") Integer userId) {
        List<PlantSummaryDto> plantas = plantasService.listar(userId);
        return ResponseEntity.ok(plantas);
    }

    /** Aplica un cuidado a la planta. */
    @PostMapping("/{id}/cuidados")
    public ResponseEntity<PlantSummaryDto> cuidar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario,
            @RequestBody Map<String, String> body) {
        String accion = body.get("accion");
        if (accion == null) {
            return ResponseEntity.badRequest().build();
        }
        PlantSummaryDto actualizado = plantasService.cuidar(userId, idPlantaUsuario, accion);
        return ResponseEntity.ok(actualizado);
    }

    /** Compra un abono de la tienda y lo usa: +1 dia de crecimiento (409 si no corresponde, 402 sin monedas). */
    @PostMapping("/{id}/abono-tienda")
    public ResponseEntity<PlantSummaryDto> abonoDeTienda(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        abonoService.abonar(userId, idPlantaUsuario);
        return ResponseEntity.ok(plantasService.resumen(userId, idPlantaUsuario));
    }

    /** Temas de fondo que se le pueden poner a la planta (el predeterminado y los comprados), con el que tiene. */
    @GetMapping("/{id}/fondos")
    public List<TemaFondoDto> fondos(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return fondosPlantaService.temas(userId, idPlantaUsuario);
    }

    /** Pone un tema de fondo a la planta. Body: { "idTema": n } (o null para volver al de siempre). 409 si no lo tiene. */
    @PutMapping("/{id}/fondo")
    public List<TemaFondoDto> ponerFondo(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario,
            @RequestBody Map<String, Integer> body) {
        return fondosPlantaService.aplicar(userId, idPlantaUsuario, body.get("idTema"));
    }

    /** Previene el evento activo en la planta (409 si no hay evento o la planta ya murio). */
    @PostMapping("/{id}/prevenir")
    public ResponseEntity<PlantSummaryDto> prevenir(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return ResponseEntity.ok(plantasService.prevenir(userId, idPlantaUsuario));
    }

    /** Nombre anterior de /prevenir; se conserva para las apps que aun lo usan. */
    @PostMapping("/{id}/refugio")
    public ResponseEntity<PlantSummaryDto> refugiar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return ResponseEntity.ok(plantasService.prevenir(userId, idPlantaUsuario));
    }

    /** Revive una planta muerta a cambio de hojas (409 si no esta muerta, 402 si no alcanzan). */
    @PostMapping("/{id}/revivir")
    public ResponseEntity<PlantSummaryDto> revivir(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return ResponseEntity.ok(plantasService.revivir(userId, idPlantaUsuario));
    }

    /** Marca como vista la animacion del evento que golpeo a la planta. */
    @PostMapping("/{id}/golpe-visto")
    public ResponseEntity<PlantSummaryDto> golpeVisto(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return ResponseEntity.ok(plantasService.golpeVisto(userId, idPlantaUsuario));
    }

    /** Activa una planta digital (cambia su etapa). */
    @PostMapping("/{id}/activar")
    public ResponseEntity<PlantSummaryDto> activar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        PlantSummaryDto activado = plantasService.activar(userId, idPlantaUsuario);
        return ResponseEntity.ok(activado);
    }

    /** Devuelve la planta digital activa del usuario (204 si no tiene ninguna). */
    @GetMapping("/activa")
    public ResponseEntity<PlantSummaryDto> activa(
            @RequestHeader("X-User-Id") Integer userId) {
        return plantasService.activa(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}