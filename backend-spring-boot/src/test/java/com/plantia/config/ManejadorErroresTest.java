package com.plantia.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

class ManejadorErroresTest {

    private final ManejadorErrores manejador = new ManejadorErrores();

    @Test
    void metodoNoPermitidoDa405yNo500() {
        var r = manejador.inesperado(new HttpRequestMethodNotSupportedException("GET"));
        assertEquals(405, r.getStatusCode().value());
    }

    @Test
    void rutaInexistenteDa404() {
        var r = manejador.inesperado(new NoResourceFoundException(HttpMethod.GET, "api/v1/nada"));
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void errorPropioSigueSiendo500() {
        var r = manejador.inesperado(new IllegalStateException("fallo"));
        assertEquals(500, r.getStatusCode().value());
        assertEquals("Error interno del servidor", r.getBody().get("error"));
    }
}
