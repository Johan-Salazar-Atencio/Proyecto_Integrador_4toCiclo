package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Solo se guardan imagenes reales, con la extension segun su contenido. */
class StorageServiceTest {

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};

    @TempDir
    Path dir;

    private StorageService storage;

    @BeforeEach
    void preparar() {
        storage = new StorageService();
        ReflectionTestUtils.setField(storage, "imagesDir", dir.toString());
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "http://x/files");
    }

    @Test
    void htmlDisfrazadoDeFotoSeRechaza() {
        var html = new MockMultipartFile("foto", "planta.jpg", "image/jpeg",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> storage.save(html))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void jpgConNombreHtmlSeGuardaComoJpg() throws Exception {
        String url = storage.save(new MockMultipartFile("foto", "x.html", "text/html", JPG));
        assertThat(url).startsWith("http://x/files/scan_").endsWith(".jpg");
        try (var archivos = Files.list(dir)) {
            assertThat(archivos).hasSize(1);
        }
    }

    @Test
    void borrarNoSaleDeLaCarpeta() throws Exception {
        String url = storage.save(new MockMultipartFile("foto", "a.jpg", "image/jpeg", JPG));
        storage.delete("http://x/files/../../fuera.txt");
        storage.delete(url);
        try (var archivos = Files.list(dir)) {
            assertThat(archivos).isEmpty();
        }
    }
}
