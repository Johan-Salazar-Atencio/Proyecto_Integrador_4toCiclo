package com.plantia.util;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Freno a la fuerza bruta del login (QA-27): tras {@code maximo} claves equivocadas seguidas para la misma clave
 * (el correo), queda bloqueada durante {@code espera}. Un login correcto limpia el contador. En memoria: con varias
 * instancias de Spring cada una cuenta lo suyo, que para este proyecto basta.
 */
public class IntentosLogin {

    private record Estado(int fallos, long desdeMs) { }

    private final int maximo;
    private final long esperaMs;
    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public IntentosLogin(int maximo, Duration espera) {
        this.maximo = maximo;
        this.esperaMs = espera.toMillis();
    }

    public void exigirPermitido(String clave) {
        exigirPermitido(clave, System.currentTimeMillis());
    }

    void exigirPermitido(String clave, long ahoraMs) {
        Estado e = clave == null ? null : estados.get(clave);
        if (e == null) {
            return;
        }
        if (ahoraMs - e.desdeMs() >= esperaMs) {
            estados.remove(clave, e);
            return;
        }
        if (e.fallos() >= maximo) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo.");
        }
    }

    public void fallo(String clave) {
        fallo(clave, System.currentTimeMillis());
    }

    void fallo(String clave, long ahoraMs) {
        if (clave == null) {
            return;
        }
        estados.compute(clave, (k, e) -> e == null || ahoraMs - e.desdeMs() >= esperaMs
                ? new Estado(1, ahoraMs)
                : new Estado(e.fallos() + 1, e.desdeMs()));
    }

    public void exito(String clave) {
        if (clave != null) {
            estados.remove(clave);
        }
    }
}
