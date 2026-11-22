package com.plantia.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.plantia.services.LogroService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Red de seguridad de {@link LogrosTrasLaAccion}: si una accion hizo commit pero la respuesta no tenia cuerpo
 * (204) o termino en error, evalua aqui lo que quedo anotado, ya sin ninguna conexion tomada. Lo desbloqueado
 * llega luego por GET /logros/pendientes.
 */
@Component
public class LogrosPendientesFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogrosPendientesFilter.class);

    private final LogroService logroService;

    public LogrosPendientesFilter(LogroService logroService) {
        this.logroService = logroService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                logroService.evaluarPendientesDeLaPeticion();
            } catch (RuntimeException e) {
                log.error("No se pudieron evaluar los logros pendientes de la peticion", e);
            }
        }
    }
}
