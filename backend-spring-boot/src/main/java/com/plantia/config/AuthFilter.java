package com.plantia.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantia.models.Usuario;
import com.plantia.repositories.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cierra la suplantacion por header X-User-Id: toda peticion a /api/v1/*
 * (excepto /api/v1/auth/*) debe incluir un X-Session-Token valido para el
 * usuario indicado. Sin token o con token que no coincide -> 401.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthFilter(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/") || path.startsWith("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader("X-User-Id");
        String token = request.getHeader("X-Session-Token");
        int userId;
        try {
            userId = Integer.parseInt(userIdHeader == null ? "" : userIdHeader.trim());
        } catch (NumberFormatException e) {
            rechazar(response, "Falta o es inválido X-User-Id");
            return;
        }

        Optional<Usuario> opt = usuarioRepo.findById(userId);
        if (opt.isEmpty()) {
            rechazar(response, "Usuario no encontrado");
            return;
        }
        Usuario u = opt.get();
        String stored = u.getSessionToken();
        if (stored == null || stored.isBlank() || token == null || !stored.equals(token)) {
            rechazar(response, "Sesión inválida o expirada. Inicia sesión de nuevo.");
            return;
        }

        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), java.util.Map.of("error", mensaje));
    }
}
