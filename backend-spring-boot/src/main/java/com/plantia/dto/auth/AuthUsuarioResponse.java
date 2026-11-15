package com.plantia.dto.auth;

public record AuthUsuarioResponse(
        Integer id,
        String nombre,
        String email,
        String rol,
        String sessionToken
) {
}
