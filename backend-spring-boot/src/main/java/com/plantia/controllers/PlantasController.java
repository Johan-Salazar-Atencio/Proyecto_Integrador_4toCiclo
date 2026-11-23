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
import com.plantia.services.MacetasService;
import com.plantia.services.PlantasService;

@RestController
@RequestMapping("/api/v1/plantas")
public class PlantasController {

    private final PlantasService plantasService;
    private final AbonoService abonoService;
    private final FondosPlantaService fondosPlantaService;
    private final MacetasService macetasService;

    public PlantasController(PlantasService plantasService, AbonoService abonoService,
                             FondosPlantaService fondosPlantaService, MacetasService macetasService) {
        this.plantasService = plantasService;
        this.abonoService = abonoService;
        this.fondosPlantaService = fondosPlantaService;
        this.macetasService = macetasService;
    }

    /**
     * Digitaliza una planta en "Mi jardin" y la deja como activa.
     * Body: { "idEscaneo": n } o { "idCatalogo": n } (de una especie que el usuario ya escaneo). Si la especie ya estaba
     * en su jardin no se reinicia: responde la planta tal cual con "yaEnJardin": true.
     */
    @PostMapping
    public ResponseEntity<PlantSummaryDto> crear(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer idEscaneo = entero(body.get("idEscaneo"));
        Integer idCatalogo = entero(body.get("idCatalogo"));
        if (idEscaneo == null && idCatalogo == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Falta idEscaneo o idCatalogo");
        }
        // "apariencia" del cuerpo se ignora (QA-12): la decide el escaneo guardado en el servidor
        return ResponseEntity.ok(plantasService.digitalizar(userId, idEscaneo, idCatalogo));
    }

    /** Elige la planta que se muestra en "Mi jardin". */
    @PostMapping("/{id}/seleccionar")
    public PlantSummaryDto seleccionar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return plantasService.seleccionar(userId, idPlantaUsuario);
    }

    /**
     * Un id del cuerpo: null si no viene; 400 si no es un entero de verdad (QA-30: antes 1.9 se tomaba como 1 y 2^40 se
     * truncaba a otro id; un texto como "1" daba 400 sin cuerpo).
     */
    private static Integer entero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Integer i) {
            return i;
        }
        if ((valor instanceof Long || valor instanceof java.math.BigInteger) && ((Number) valor).longValue() == ((Number) valor).doubleValue()
                && ((Number) valor).longValue() >= Integer.MIN_VALUE && ((Number) valor).longValue() <= Integer.MAX_VALUE) {
            return ((Number) valor).intValue();
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Los ids deben ser numeros enteros");
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
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Falta la accion: REGAR, SOL o ABONAR");
        }
        PlantSummaryDto actualizado = plantasService.cuidar(userId, idPlantaUsuario, accion);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Compra un abono de la tienda y lo usa (medio dia, 1 o 3 dias de crecimiento segun el abono).
     * Body opcional: { "idItem": n }; sin body se usa el abono mas barato. 409 si no corresponde, 402 sin monedas.
     */
    @PostMapping("/{id}/abono-tienda")
    public ResponseEntity<PlantSummaryDto> abonoDeTienda(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario,
            @RequestBody(required = false) Map<String, Integer> body) {
        abonoService.abonar(userId, idPlantaUsuario, body != null ? body.get("idItem") : null);
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

    /**
     * Pone una maceta que el usuario tiene a las plantas que elija. Body: { "idItem": n, "ids": [1, 2] }.
     * 409 si no tiene la maceta o una planta esta en el baul.
     */
    @PutMapping("/macetas")
    public List<PlantSummaryDto> ponerMaceta(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer idItem = entero(body.get("idItem"));
        List<Integer> ids = body.get("ids") instanceof List<?> lista
                ? lista.stream().map(PlantasController::entero).filter(java.util.Objects::nonNull).toList()
                : List.of();
        if (idItem == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Falta idItem");
        }
        return macetasService.aplicar(userId, idItem, ids);
    }

    /** Guarda la planta en el baul (congelada). 409 si esta golpeada por un evento. */
    @PostMapping("/{id}/baul")
    public ResponseEntity<PlantSummaryDto> guardarEnBaul(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return ResponseEntity.ok(plantasService.guardarEnBaul(userId, idPlantaUsuario));
    }

    /** Saca la planta del baul (409 si el jardin ya tiene el maximo de plantas activas). */
    @PostMapping("/{id}/sacar-del-baul")
    public ResponseEntity<PlantSummaryDto> sacarDelBaul(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario) {
        return ResponseEntity.ok(plantasService.sacarDelBaul(userId, idPlantaUsuario));
    }

    /** Cambia el apodo de la planta. Body: { "apodo": "texto" } (vacio = nombre comun de la especie). */
    @PutMapping("/{id}/apodo")
    public ResponseEntity<PlantSummaryDto> renombrar(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer idPlantaUsuario,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(plantasService.renombrar(userId, idPlantaUsuario, body.get("apodo")));
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

    /** Nombre anterior de /seleccionar: ya no cambia la fase de la planta (QA-06). */
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