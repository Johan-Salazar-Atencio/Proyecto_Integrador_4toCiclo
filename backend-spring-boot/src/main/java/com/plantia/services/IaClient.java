package com.plantia.services;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        try {
            ByteArrayResource resource = new ByteArrayResource(foto.getBytes()) {
                @Override
                public String getFilename() {
                    return foto.getOriginalFilename() != null ? foto.getOriginalFilename() : "scan.jpg";
                }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("imagen", resource);
            body.add("provider", provider);

            return restClient.post()
                    .uri("/api/ia/identificar")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IaResultDto.class);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen para la IA", e);
        }
    }

    /** Resultado por defecto cuando el motor IA no responde. */
    public static IaResultDto fallback() {
        return new IaResultDto("Planta desconocida", null, 0.0, List.of(), "{\"error\":\"ia_no_disponible\"}");
    }
}
