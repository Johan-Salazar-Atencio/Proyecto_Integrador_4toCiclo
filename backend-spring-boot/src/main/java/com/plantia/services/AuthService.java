package com.plantia.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.auth.AuthLoginRequest;
import com.plantia.dto.auth.AuthRegistroRequest;
import com.plantia.dto.auth.AuthUsuarioResponse;
import com.plantia.models.Usuario;
import com.plantia.repositories.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // Hash BCrypt valido usado para comparar cuando el email no existe, para que
    // el tiempo de respuesta no revele si el correo esta registrado.
    private static final String DUMMY_HASH = "$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy";

    public AuthService(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional
    public AuthUsuarioResponse registrar(AuthRegistroRequest request) {
        String emailNormalizado = normalizarEmail(request.email());
        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo inválido");
        }

        if (usuarioRepo.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }

        String nombre = request.nombre() == null ? "" : request.nombre().trim();
        // Evitar XSS almacenado: se eliminan etiquetas HTML del nombre.
        nombre = nombre.replaceAll("<[^>]*>", "").trim();
        if (nombre.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener al menos 2 caracteres");
        }

        String password = request.password() == null ? "" : request.password();
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres");
        }
        // Nueva validación: BCrypt limita a 72 bytes, rechazamos >72 con 400
        if (password.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede tener más de 72 caracteres");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol("USER");
        usuario.setNivel(1);
        usuario.setExperienciaXp(0);
        usuario.setMonedas(100);
        usuario.setRachaDias(0);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());

        Usuario guardado = usuarioRepo.save(usuario);
        String token = generarToken();
        guardado.setSessionToken(token);
        usuarioRepo.save(guardado);
        return new AuthUsuarioResponse(guardado.getIdUsuario(), guardado.getNombre(), guardado.getEmail(), guardado.getRol(), token);
    }

    @Transactional
    public AuthUsuarioResponse login(AuthLoginRequest request) {
        String emailNormalizado = normalizarEmail(request.email());
        String password = request.password() == null ? "" : request.password();

        if (emailNormalizado == null || emailNormalizado.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepo.findByEmail(emailNormalizado)
                .orElse(null);

        if (usuario == null) {
            // Comparamos contra un hash valido para no revelar (por tiempo) si
            // el email existe. Siempre lanzamos el mismo error.
            passwordEncoder.matches(password, DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        String token = generarToken();
        usuario.setSessionToken(token);
        usuarioRepo.save(usuario);

        return new AuthUsuarioResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(), token);
    }

    private String generarToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
