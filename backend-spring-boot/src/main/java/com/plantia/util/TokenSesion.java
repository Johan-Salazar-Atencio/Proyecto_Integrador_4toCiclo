package com.plantia.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Tokens de sesion. Al cliente se le da el token; en la BD solo se guarda su
 * SHA-256, asi una fuga de la BD no permite usar las sesiones. Django usa el
 * mismo esquema para el panel admin (catalog/admin_views.py).
 */
public final class TokenSesion {

    /** Cuanto dura una sesion desde el login. */
    public static final Duration DURACION = Duration.ofDays(30);

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenSesion() {
    }

    /** Token nuevo: 128 bits aleatorios en hex (32 caracteres). */
    public static String generar() {
        byte[] bytes = new byte[16];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** SHA-256 en hex (64 caracteres), que es lo que se guarda en usuarios.session_token. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Compara en tiempo constante el hash guardado con el token recibido. */
    public static boolean coincide(String hashGuardado, String tokenRecibido) {
        if (hashGuardado == null || hashGuardado.isBlank() || tokenRecibido == null || tokenRecibido.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                hashGuardado.getBytes(StandardCharsets.US_ASCII),
                hash(tokenRecibido).getBytes(StandardCharsets.US_ASCII));
    }
}
