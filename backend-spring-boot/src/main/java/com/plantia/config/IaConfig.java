package com.plantia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clave compartida con el motor de IA de Django (header X-Ia-Clave).
 * Django solo acepta llamadas con esa clave, o desde la misma maquina si no hay
 * clave configurada (ver backend-django/ia_engine/permisos.py).
 * Se aplica al RestClient.Builder que usa IaClient, unico cliente HTTP saliente.
 */
@Configuration
public class IaConfig {

    @Bean
    RestClientCustomizer claveInternaIa(@Value("${IA_CLAVE_INTERNA:}") String clave) {
        return builder -> {
            if (!clave.isBlank()) {
                builder.defaultHeader("X-Ia-Clave", clave);
            }
        };
    }
}
