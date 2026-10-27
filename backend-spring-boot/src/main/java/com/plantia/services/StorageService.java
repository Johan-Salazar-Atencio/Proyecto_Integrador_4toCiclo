package com.plantia.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guarda las fotos de escaneo en el volumen local (Docker) y devuelve
 * la URL publica servida por /files/**.
 */
@Service
public class StorageService {

    @Value("${app.storage.images-dir:storage/images}")
    private String imagesDir;

    @Value("${app.storage.public-base-url:http://localhost:8080/files}")
    private String publicBaseUrl;

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo de imagen vacio");
        }
        try {
            Path dir = Paths.get(imagesDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String ext = extension(file.getOriginalFilename());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String name = "scan_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            Path target = dir.resolve(name);
            // Copia (no transferTo, que mueve el temporal y deja ilegible el
            // MultipartFile para la llamada posterior a la IA).
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return publicBaseUrl + "/" + name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la imagen: " + e.getMessage());
        }
    }

    private String extension(String original) {
        if (original == null) {
            return ".jpg";
        }
        int dot = original.lastIndexOf('.');
        return dot >= 0 ? original.substring(dot) : ".jpg";
    }
}
