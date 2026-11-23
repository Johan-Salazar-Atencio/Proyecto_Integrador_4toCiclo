package com.plantia.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** QA-27: freno a la fuerza bruta del login. */
class IntentosLoginTest {

    private final IntentosLogin intentos = new IntentosLogin(3, Duration.ofMinutes(15));

    @Test
    void trasVariosFallosSeguidosSeBloqueaConUn429() {
        for (int i = 0; i < 3; i++) {
            intentos.exigirPermitido("a@b.c", 1_000);
            intentos.fallo("a@b.c", 1_000);
        }
        assertThatThrownBy(() -> intentos.exigirPermitido("a@b.c", 2_000))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        intentos.exigirPermitido("otro@b.c", 2_000); // otro correo no se bloquea
    }

    @Test
    void pasadaLaEsperaOConUnLoginCorrectoSeVuelveAPoder() {
        for (int i = 0; i < 3; i++) {
            intentos.fallo("a@b.c", 0);
        }
        intentos.exigirPermitido("a@b.c", Duration.ofMinutes(15).toMillis());

        for (int i = 0; i < 2; i++) {
            intentos.fallo("x@b.c", 0);
        }
        intentos.exito("x@b.c");
        intentos.fallo("x@b.c", 0);
        intentos.exigirPermitido("x@b.c", 1);
    }
}
