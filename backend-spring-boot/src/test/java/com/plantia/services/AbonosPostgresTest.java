package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.AbonosDto;

/**
 * Inventario de abonos (V35) contra un Postgres REAL, como LogrosPostgresTest: solo corre con PLANTIA_TEST_DB_URL
 * y contra una base cuyo nombre diga "prueba" o "test". Flyway aplica todas las migraciones desde cero.
 */
@EnabledIfEnvironmentVariable(named = "PLANTIA_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.eventos.intervalo-ms=3600000",
        "spring.flyway.locations=filesystem:../db/migration",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.datasource.hikari.connection-timeout=5000"
})
class AbonosPostgresTest {

    @Autowired private AbonoService abonoService;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void baseDePruebas(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("PLANTIA_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> envO("PLANTIA_TEST_DB_USER", "plantia"));
        r.add("spring.datasource.password", () -> envO("PLANTIA_TEST_DB_PASSWORD", "plantia"));
    }

    @BeforeAll
    static void soloBaseDePruebas() {
        String url = System.getenv("PLANTIA_TEST_DB_URL");
        String base = url.replaceAll("\\?.*$", "").replaceAll("^.*/", "").toLowerCase();
        assumeTrue(base.contains("prueba") || base.contains("test"),
                "Por seguridad solo corre contra una base de pruebas (su nombre debe decir prueba o test): " + base);
    }

    private static String envO(String nombre, String defecto) {
        String v = System.getenv(nombre);
        return v == null || v.isBlank() ? defecto : v;
    }

    private int usuario(int monedas) {
        String s = UUID.randomUUID().toString().substring(0, 8);
        return jdbc.queryForObject("INSERT INTO usuarios (nombre, email, password_hash, monedas, experiencia_xp, nivel, racha_dias) "
                + "VALUES (?, ?, 'x', ?, 0, 1, 0) RETURNING id_usuario", Integer.class,
                "Abono " + s, "abono-" + s + "@plantia.test", monedas);
    }

    private int planta(int idUsuario) {
        String s = UUID.randomUUID().toString().substring(0, 8);
        int cat = jdbc.queryForObject("INSERT INTO plantas_catalogo (nombre_comun, nombre_cientifico, frecuencia_riego_dias, "
                + "frecuencia_sol_horas, sprite_base_url) VALUES ('Prueba', ?, 3, 4, 'x') RETURNING id_catalogo",
                Integer.class, "Planta abono " + s);
        return jdbc.queryForObject("INSERT INTO plantas_usuario (id_usuario, id_catalogo, apodo, salud_porcentaje, "
                + "hidratacion_porcentaje, sol_porcentaje, etapa_crecimiento, fecha_plantado, updated_at) "
                + "VALUES (?, ?, 'a', 90, 60, 70, 'SEMILLA', now(), now()) RETURNING id_planta_usuario",
                Integer.class, idUsuario, cat);
    }

    private int item(String codigo) {
        return jdbc.queryForObject("SELECT id_item FROM items_tienda WHERE tipo = 'ABONO' AND codigo = ?", Integer.class, codigo);
    }

    private int existencias(int u, int item) {
        List<Integer> r = jdbc.queryForList("SELECT cantidad FROM abonos_usuario WHERE id_usuario = ? AND id_item = ?",
                Integer.class, u, item);
        return r.isEmpty() ? 0 : r.get(0);
    }

    private int monedas(int u) {
        return jdbc.queryForObject("SELECT monedas FROM usuarios WHERE id_usuario = ?", Integer.class, u);
    }

    @Test
    void v35AplicadaConLosTresAbonosYSinCantidadesNegativas() {
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '35'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForList("SELECT codigo FROM items_tienda WHERE tipo = 'ABONO' ORDER BY precio_monedas", String.class))
                .containsExactly("ABONO_MEDIO", "ABONO_1", "ABONO_3");
        int u = usuario(0);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO abonos_usuario (id_usuario, id_item, cantidad) VALUES (?, ?, -1)",
                u, item("ABONO_1"))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void comprarYUsarDeVerdadContraLaBase() {
        int u = usuario(200);
        int p = planta(u);
        int organico = item("ABONO_1");

        AbonosDto inv = abonoService.comprar(u, organico, 2);
        assertThat(monedas(u)).isEqualTo(140);
        assertThat(existencias(u, organico)).isEqualTo(2);
        assertThat(inv.abonos()).extracting(AbonosDto.Abono::cantidad).containsExactly(0, 2, 0);
        assertThat(abonoService.comprar(u, organico, null).abonos().get(1).cantidad()).isEqualTo(3);

        abonoService.usar(u, p, organico);
        assertThat(existencias(u, organico)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT dias_cuidados FROM plantas_usuario WHERE id_planta_usuario = ?", Integer.class, p))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT abonos_total FROM usuarios WHERE id_usuario = ?", Integer.class, u)).isEqualTo(1);
        // El mismo dia no se puede otro: 409 y no se descuenta
        assertThatThrownBy(() -> abonoService.usar(u, p, organico)).isInstanceOf(ResponseStatusException.class);
        assertThat(existencias(u, organico)).isEqualTo(2);
    }

    @Test
    void sinExistenciasNoSeUsaYNoQuedaNegativo() {
        int u = usuario(0);
        int p = planta(u);
        assertThatThrownBy(() -> abonoService.usar(u, p, item("ABONO_3"))).isInstanceOf(ResponseStatusException.class);
        assertThat(existencias(u, item("ABONO_3"))).isZero();
    }

    @Test
    void carreraDeUsosEnLaMismaPlantaSoloUnoPasaYSoloUnoSeDescuenta() throws Exception {
        int u = usuario(500);
        int p = planta(u);
        int organico = item("ABONO_1");
        abonoService.comprar(u, organico, 5);

        int hilos = 6;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> r = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            r.add(pool.submit(() -> {
                salida.await();
                try {
                    abonoService.usar(u, p, organico);
                    return true;
                } catch (ResponseStatusException e) {
                    return false;
                }
            }));
        }
        salida.countDown();
        int ok = 0;
        for (Future<Boolean> f : r) {
            ok += f.get(60, TimeUnit.SECONDS) ? 1 : 0;
        }
        pool.shutdown();

        assertThat(ok).isEqualTo(1);
        assertThat(existencias(u, organico)).isEqualTo(4);
    }

    @Test
    void carreraDeComprasNoGastaMasDeLoQueHay() throws Exception {
        int u = usuario(60); // alcanza para 2 abonos organicos (30)
        int organico = item("ABONO_1");

        int hilos = 6;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> r = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            r.add(pool.submit(() -> {
                salida.await();
                try {
                    abonoService.comprar(u, organico, 1);
                    return true;
                } catch (ResponseStatusException e) {
                    return false;
                }
            }));
        }
        salida.countDown();
        int ok = 0;
        for (Future<Boolean> f : r) {
            ok += f.get(60, TimeUnit.SECONDS) ? 1 : 0;
        }
        pool.shutdown();

        assertThat(ok).isEqualTo(2);
        assertThat(monedas(u)).isZero();
        assertThat(existencias(u, organico)).isEqualTo(2);
    }
}
