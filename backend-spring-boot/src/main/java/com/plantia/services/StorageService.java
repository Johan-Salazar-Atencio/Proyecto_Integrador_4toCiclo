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
            // La extension sale del contenido real, nunca del nombre que manda el cliente:
            // asi no se puede subir un .html/.svg con JavaScript y servirlo desde /files/.
            String ext = extensionSegunContenido(cabecera(file));
            if (ext == null) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Solo se aceptan imagenes JPG, PNG o WEBP");
            }

            Path dir = Paths.get(imagesDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

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

    /** Borra una foto guardada (por ejemplo, si el escaneo falla despues de guardarla). */
    public void delete(String url) {
        if (url == null || !url.startsWith(publicBaseUrl + "/")) {
            return;
        }
        String name = url.substring(publicBaseUrl.length() + 1);
        Path dir = Paths.get(imagesDir).toAbsolutePath().normalize();
        Path target = dir.resolve(name).normalize();
        if (!target.startsWith(dir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Una foto huerfana no debe tapar el error original
        }
    }

    private static byte[] cabecera(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(12);
        }
    }

    /** Extension segun los "magic bytes" del archivo; null si no es JPG, PNG ni WEBP. */
    static String extensionSegunContenido(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return ".png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
