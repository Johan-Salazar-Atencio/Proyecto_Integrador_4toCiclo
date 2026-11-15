package com.plantia.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Todas las respuestas de error de la API tienen la misma forma que las de AuthFilter:
 * {"error": "mensaje para el usuario"}. Antes, una IllegalArgumentException acababa
 * en un 500 y el mensaje de ResponseStatusException no llegaba al cliente.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> estado(ResponseStatusException e) {
        String mensaje = e.getReason() != null ? e.getReason() : "Error " + e.getStatusCode().value();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", mensaje));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Datos invalidos"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<Map<String, String>> sinCabecera(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Falta la cabecera " + e.getHeaderName()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> archivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "La imagen supera los 15 MB"));
    }

    /** Cuerpo ilegible (JSON mal formado) o parametro con tipo equivocado ("abc" donde va un id). */
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    ResponseEntity<Map<String, String>> peticionMalFormada(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Peticion mal formada"));
    }

    /**
     * Cualquier otro error. Las excepciones propias de Spring MVC (metodo no permitido,
     * ruta inexistente, tipo de contenido no soportado...) ya traen su codigo 4xx: se
     * respeta. El resto se registra completo y al cliente solo le llega un mensaje generico.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> inesperado(Exception e) {
        if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            HttpStatusCode codigo = er.getStatusCode();
            String mensaje = er.getBody().getDetail() != null ? er.getBody().getDetail() : "Error " + codigo.value();
            return ResponseEntity.status(codigo).body(Map.of("error", mensaje));
        }
        log.error("Error no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno del servidor"));
    }
}
