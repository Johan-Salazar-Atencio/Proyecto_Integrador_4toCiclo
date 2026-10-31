package com.plantia.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS abierto (sin auth en este sprint) y servido de fotos de escaneo
 * desde el volumen local.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.images-dir:storage/images}")
    private String imagesDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
        registry.addMapping("/files/**")
                .allowedOriginPatterns("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(imagesDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(dir.toUri().toString());
    }
}
