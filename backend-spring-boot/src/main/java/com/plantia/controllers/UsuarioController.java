package com.plantia.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public UserSummaryDto me(@RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        return usuarioService.me(userId);
    }
}
