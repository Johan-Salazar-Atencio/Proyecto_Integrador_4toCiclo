package com.plantia.services;

import java.time.LocalDateTime;

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
        if (nombre.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener al menos 2 caracteres");
        }

        String password = request.password() == null ? "" : request.password();
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres");
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
        return new AuthUsuarioResponse(guardado.getIdUsuario(), guardado.getNombre(), guardado.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthUsuarioResponse login(AuthLoginRequest request) {
        String emailNormalizado = normalizarEmail(request.email());
        String password = request.password() == null ? "" : request.password();

        if (emailNormalizado == null || emailNormalizado.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepo.findByEmail(emailNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        return new AuthUsuarioResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getEmail());
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
