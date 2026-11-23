package com.plantia.util;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Freno a la fuerza bruta del login (QA-27, QA-37, QA-38).
 * <ul>
 *   <li>El intento se cuenta ANTES de comprobar la clave y de forma atomica: 20 intentos simultaneos ya no pasan 14
 *       veces el tope de 5.</li>
 *   <li>La cuenta es por correo + IP: alguien que prueba claves desde otra IP ya no deja fuera a la duena de la cuenta
 *       (ni al admin). Ademas hay un tope por IP para todos los correos, contra el que prueba muchos correos.</li>
 *   <li>Un login correcto borra la cuenta de su correo + IP. Las entradas vencidas se limpian solas.</li>
 * </ul>
 * En memoria: con varias instancias de Spring cada una cuenta lo suyo.
 */
public class IntentosLogin {

    private record Estado(int intentos, long desdeMs) { }

    static final int LIMPIAR_DESDE = 10_000;

    private final int maximo;
    private final int maximoPorIp;
    private final long esperaMs;
    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public IntentosLogin(int maximo, int maximoPorIp, Duration espera) {
        this.maximo = maximo;
        this.maximoPorIp = maximoPorIp;
        this.esperaMs = espera.toMillis();
    }

    /** Cuenta un intento de login de ese correo desde esa IP; 429 si ya se pasaron los topes (no se mira la clave). */
    public void intentar(String email, String ip) {
        intentar(email, ip, System.currentTimeMillis());
    }

    void intentar(String email, String ip, long ahoraMs) {
        if (estados.size() > LIMPIAR_DESDE) {
            estados.entrySet().removeIf(e -> ahoraMs - e.getValue().desdeMs() >= esperaMs);
        }
        int porCorreo = sumar("c:" + email + "|" + ip, ahoraMs);
        int porIp = sumar("i:" + ip, ahoraMs);
        if (porCorreo > maximo || porIp > maximoPorIp) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo.");
        }
    }

    /** Login correcto: se borra la cuenta de ese correo desde esa IP. */
    public void exito(String email, String ip) {
        estados.remove("c:" + email + "|" + ip);
    }

    int tamano() {
        return estados.size();
    }

    private int sumar(String clave, long ahoraMs) {
        return estados.compute(clave, (k, e) -> e == null || ahoraMs - e.desdeMs() >= esperaMs
                ? new Estado(1, ahoraMs)
                : new Estado(e.intentos() + 1, e.desdeMs())).intentos();
    }
}
