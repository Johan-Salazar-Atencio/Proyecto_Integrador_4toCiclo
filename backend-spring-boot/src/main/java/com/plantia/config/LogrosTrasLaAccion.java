package com.plantia.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.plantia.services.LogroService;

/**
 * Evalua los logros que dejaron anotados las acciones de la peticion justo antes de escribir el cuerpo.
 * En este punto el servicio ya hizo commit y devolvio su conexion al pool (open-in-view esta apagado), asi que
 * la evaluacion usa UNA sola conexion; y como la respuesta aun no se serializa, {@code logrosNuevos} llega lleno.
 */
@RestControllerAdvice
public class LogrosTrasLaAccion implements ResponseBodyAdvice<Object> {

    private final LogroService logroService;

    public LogrosTrasLaAccion(LogroService logroService) {
        this.logroService = logroService;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        logroService.evaluarPendientesDeLaPeticion(); // nunca lanza: un fallo de logros no cambia la respuesta
        return body;
    }
}
