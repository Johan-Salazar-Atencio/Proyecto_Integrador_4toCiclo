package com.plantia.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** QA-27, QA-37 y QA-38: freno a la fuerza bruta del login. */
class IntentosLoginTest {

    private final IntentosLogin intentos = new IntentosLogin(3, 10, Duration.ofMinutes(15));

    private static void es429(Runnable r) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void trasElTopeSeBloqueaConUn429YPasadaLaEsperaVuelveAPoder() {
        for (int i = 0; i < 3; i++) {
            intentos.intentar("a@b.c", "1.1.1.1", 1_000);
        }
        es429(() -> intentos.intentar("a@b.c", "1.1.1.1", 2_000));
        intentos.intentar("a@b.c", "1.1.1.1", 1_000 + Duration.ofMinutes(15).toMillis());
    }

    @Test
    void qa37LosFallosDesdeOtraIpNoBloqueanALaDuenaDeLaCuenta() {
        for (int i = 0; i < 3; i++) {
            intentos.intentar("victima@b.c", "6.6.6.6", 1_000);
        }
        es429(() -> intentos.intentar("victima@b.c", "6.6.6.6", 1_000));
        intentos.intentar("victima@b.c", "2.2.2.2", 1_000); // la victima, desde su IP, puede entrar
    }

    @Test
    void qa37UnaIpQuePruebaMuchosCorreosTambienSeFrena() {
        for (int i = 0; i < 10; i++) {
            intentos.intentar("c" + i + "@b.c", "9.9.9.9", 1_000);
        }
        es429(() -> intentos.intentar("otro@b.c", "9.9.9.9", 1_000));
    }

    @Test
    void unLoginCorrectoLimpiaSuCuenta() {
        intentos.intentar("x@b.c", "1.1.1.1", 0);
        intentos.intentar("x@b.c", "1.1.1.1", 0);
        intentos.exito("x@b.c", "1.1.1.1");
        intentos.intentar("x@b.c", "1.1.1.1", 1);
        intentos.intentar("x@b.c", "1.1.1.1", 1);
    }

    @Test
    void qa37VeinteIntentosALaVezSoloDejanPasarElTope() throws Exception {
        IntentosLogin cinco = new IntentosLogin(5, 1000, Duration.ofMinutes(15));
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger pasan = new AtomicInteger();
        List<Future<?>> f = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            f.add(pool.submit(() -> {
                salida.await();
                try {
                    cinco.intentar("a@b.c", "1.1.1.1");
                    pasan.incrementAndGet();
                } catch (ResponseStatusException e) {
                    // bloqueado
                }
                return null;
            }));
        }
        salida.countDown();
        for (Future<?> x : f) {
            x.get();
        }
        pool.shutdown();
        assertThat(pasan.get()).isEqualTo(5);
    }

    @Test
    void qa38LasEntradasVencidasSeLimpian() {
        for (int i = 0; i <= IntentosLogin.LIMPIAR_DESDE; i++) {
            intentos.intentar("c" + i + "@b.c", "ip" + i, 0);
        }
        assertThat(intentos.tamano()).isGreaterThan(IntentosLogin.LIMPIAR_DESDE);
        intentos.intentar("nuevo@b.c", "ip-nueva", Duration.ofMinutes(16).toMillis());
        assertThat(intentos.tamano()).isEqualTo(2);
    }
}
