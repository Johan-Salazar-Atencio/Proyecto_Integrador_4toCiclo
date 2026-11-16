package com.plantia.controllers;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.CambiarRegionRequest;
import com.plantia.dto.UserSummaryDto;
import com.plantia.services.UsuarioService;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Alimenta UserSummary del movil. Usuario temporal via header X-User-Id. */
    @GetMapping("/me")
    public UserSummaryDto me(@RequestHeader("X-User-Id") Integer userId) {
        return usuarioService.me(userId);
    }

    /** Sube la foto de perfil (form-data, campo "foto": JPG, PNG o WEBP) y devuelve el resumen con la nueva URL. */
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserSummaryDto cambiarFoto(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestPart("foto") MultipartFile foto) {
        return usuarioService.cambiarFoto(userId, foto);
    }

    /** Elige el invernadero de la coleccion virtual. Body: { "idItem": n }. 409 si todavia no lo compro. */
    @PutMapping("/me/invernadero")
    public UserSummaryDto cambiarInvernadero(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody java.util.Map<String, Integer> body) {
        return usuarioService.cambiarInvernadero(userId, body.get("idItem"));
    }

    /** Cambia el nombre con el que se muestra el usuario y devuelve su resumen. */
    @PutMapping("/me/nombre")
    public UserSummaryDto cambiarNombre(
            @RequestHeader("X-User-Id") Integer userId,
            @Valid @RequestBody com.plantia.dto.CambiarNombreRequest request) {
        return usuarioService.cambiarNombre(userId, request.nombre());
    }

    /** Cambia la region del usuario (decide que desastres le llegan) y devuelve su resumen. */
    @PutMapping("/me")
    public UserSummaryDto cambiarRegion(
            @RequestHeader("X-User-Id") Integer userId,
            @Valid @RequestBody CambiarRegionRequest request) {
        return usuarioService.cambiarRegion(userId, request.region());
    }
}
