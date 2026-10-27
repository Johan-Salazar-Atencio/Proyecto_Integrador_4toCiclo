package com.plantia.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.plantia.dto.auth.AuthLoginRequest;
import com.plantia.dto.auth.AuthRegistroRequest;
import com.plantia.dto.auth.AuthUsuarioResponse;
import com.plantia.services.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUsuarioResponse registro(@Valid @RequestBody AuthRegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public AuthUsuarioResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }
}
