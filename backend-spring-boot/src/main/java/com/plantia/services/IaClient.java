package com.plantia.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.plantia.dto.IaResultDto;

/**
 * Cliente HTTP hacia el motor IA (Django).
 * El proveedor real (GPT-4o / Gemini / local) se decide por config
 * (IA_PROVIDER) o automaticamente en el backend Python.
 */
@Service
public class IaClient {

    private final RestClient restClient;
    private final String provider;

    public IaClient(RestClient.Builder builder,
                    @Value("${app.ia.base-url:http://localhost:8000}") String baseUrl,
                    @Value("${app.ia.provider:auto}") String provider) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.provider = provider;
    }

    public IaResultDto identificar(MultipartFile foto) {
        byte[] imagen;
        try {
            imagen = foto.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen para la IA", e);
        }

        String filename = foto.getOriginalFilename() != null ? foto.getOriginalFilename() : "scan.jpg";
        String contentType = foto.getContentType() != null ? foto.getContentType() : "application/octet-stream";
        String boundary = "----PlantiaBoundary" + UUID.randomUUID().toString().replace("-", "");

        // Se arma el multipart a mano (byte[] con Content-Length) porque el writer
        // de Spring usa Transfer-Encoding: chunked, que el server de Django no procesa.
        byte[] body = construirMultipart(boundary, imagen, filename, contentType, provider);

        return restClient.post()
                .uri("/api/ia/identificar")
                .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                .body(body)
                .retrieve()
                .body(IaResultDto.class);
    }

    private static byte[] construirMultipart(String boundary, byte[] imagen,
                                             String filename, String contentType, String provider) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String crlf = "\r\n";
            String prefijo = "--" + boundary + crlf;

            out.write((prefijo
                    + "Content-Disposition: form-data; name=\"imagen\"; filename=\"" + filename + "\"" + crlf
                    + "Content-Type: " + contentType + crlf + crlf).getBytes(StandardCharsets.UTF_8));
            out.write(imagen);
            out.write(crlf.getBytes(StandardCharsets.UTF_8));

            out.write((prefijo
                    + "Content-Disposition: form-data; name=\"provider\"" + crlf + crlf
                    + provider + crlf).getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Resultado por defecto cuando el motor IA no responde. */
    public static IaResultDto fallback() {
        return new IaResultDto("Planta desconocida", null, 0.0, List.of(), "{\"error\":\"ia_no_disponible\"}");
    }
}
