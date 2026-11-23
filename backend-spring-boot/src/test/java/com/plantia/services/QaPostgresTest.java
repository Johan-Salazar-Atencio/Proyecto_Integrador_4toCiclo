package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/**
 * Fallas del QA profundo del 22/11 (QA-01..06, 12, 16) contra un Postgres REAL y la API por HTTP, con un motor de IA
 * falso dentro de la prueba (la especie sale del nombre del archivo: sp_Rosa.png -> "Rosa qaensis"). Igual que
 * LogrosPostgresTest: solo corre con PLANTIA_TEST_DB_URL y contra una base cuyo nombre diga "prueba" o "test".
 * Nunca llama a Gemini.
 */
@EnabledIfEnvironmentVariable(named = "PLANTIA_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.eventos.intervalo-ms=3600000",
        "spring.flyway.locations=filesystem:../db/migration",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.datasource.hikari.connection-timeout=5000"
})
class QaPostgresTest {

    private static HttpServer iaFalsa;
    private static java.nio.file.Path fotos;

    @Autowired private JdbcTemplate jdbc;
    @LocalServerPort private int puerto;
    private final HttpClient http = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(40)).build();
    private final ObjectMapper json = new ObjectMapper();

    @DynamicPropertySource
    static void entorno(DynamicPropertyRegistry r) throws Exception {
        r.add("spring.datasource.url", () -> System.getenv("PLANTIA_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> envO("PLANTIA_TEST_DB_USER", "plantia"));
        r.add("spring.datasource.password", () -> envO("PLANTIA_TEST_DB_PASSWORD", "plantia"));
        if (iaFalsa == null) {
            iaFalsa = iniciarIaFalsa();
            fotos = Files.createTempDirectory("plantia-qa-fotos");
        }
        r.add("app.ia.base-url", () -> "http://127.0.0.1:" + iaFalsa.getAddress().getPort());
        r.add("app.storage.images-dir", () -> fotos.toString());
    }

    @BeforeAll
    static void soloBaseDePruebas() {
        String url = System.getenv("PLANTIA_TEST_DB_URL");
        String base = url.replaceAll("\\?.*$", "").replaceAll("^.*/", "").toLowerCase();
        assumeTrue(base.contains("prueba") || base.contains("test"),
                "Por seguridad solo corre contra una base de pruebas (su nombre debe decir prueba o test): " + base);
    }

    @AfterAll
    static void apagar() {
        if (iaFalsa != null) {
            iaFalsa.stop(0);
        }
    }

    // ---------------------------------------------------------------- QA-01

    @Test
    void qa01EscaneosSimultaneosDeLaMismaEspecieNuevaYExistenteDanTodos200() throws Exception {
        String especie = "Nueva" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        for (int ronda = 0; ronda < 2; ronda++) { // ronda 0: especie nueva; ronda 1: ya existe en el catalogo
            List<Integer> usuarios = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                usuarios.add(usuarioConSesion());
            }
            List<HttpResponse<String>> r = aLaVez(usuarios.stream()
                    .<Callable<HttpResponse<String>>>map(u -> () -> escanear(u, "sp_" + especie + ".png")).toList());
            assertThat(r).as("ronda " + ronda).allSatisfy(x -> assertThat(x.statusCode()).as(x.body()).isEqualTo(200));
            for (int u : usuarios) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM escaneos_historial e JOIN plantas_catalogo c ON c.id_catalogo = e.id_catalogo "
                        + "WHERE e.id_usuario = ? AND c.nombre_cientifico = ?", Integer.class, u, especie + " qaensis")).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historial_progreso h JOIN reglas_progreso g ON g.id_regla = h.id_regla "
                        + "WHERE h.id_usuario = ? AND g.motivo = 'ESCANEO_NUEVA'", Integer.class, u)).isEqualTo(1);
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plantas_catalogo WHERE lower(nombre_cientifico) = lower(?)",
                Integer.class, especie + " qaensis")).isEqualTo(1);
    }

    @Test
    void qa13UnEscaneoNoPisaElNombreNiLosCuidadosQueDejoElAdmin() throws Exception {
        String especie = "Admin" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        int u = usuarioConSesion();
        assertThat(escanear(u, "sp_" + especie + ".png").statusCode()).isEqualTo(200);
        int cat = jdbc.queryForObject("SELECT id_catalogo FROM plantas_catalogo WHERE nombre_cientifico = ?", Integer.class, especie + " qaensis");
        jdbc.update("UPDATE plantas_catalogo SET nombre_comun = 'Nombre del admin' WHERE id_catalogo = ?", cat);
        List<Integer> secciones = jdbc.queryForList("SELECT id_seccion FROM cuidados_secciones WHERE id_catalogo = ? ORDER BY id_seccion", Integer.class, cat);

        assertThat(escanear(usuarioConSesion(), "sp_" + especie + ".png").statusCode()).isEqualTo(200);

        assertThat(jdbc.queryForObject("SELECT nombre_comun FROM plantas_catalogo WHERE id_catalogo = ?", String.class, cat)).isEqualTo("Nombre del admin");
        assertThat(jdbc.queryForList("SELECT id_seccion FROM cuidados_secciones WHERE id_catalogo = ? ORDER BY id_seccion", Integer.class, cat))
                .isEqualTo(secciones);
    }

    // ---------------------------------------------------------------- QA-02

    @Test
    void qa02PrimerAccesoDelDiaConGetYEscaneosSimultaneosNoDaDeadlockNi409() throws Exception {
        for (int ronda = 0; ronda < 3; ronda++) {
            int u = usuarioConSesion();
            List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                tareas.add(() -> get(u, "/api/v1/misiones"));
                int n = i;
                tareas.add(() -> escanear(u, "sp_Mision" + n + UUID.randomUUID().toString().substring(0, 4) + ".png"));
            }
            assertThat(aLaVez(tareas)).allSatisfy(x -> assertThat(x.statusCode()).as(x.body()).isEqualTo(200));

            int v = usuarioConSesion();
            List<Callable<HttpResponse<String>>> soloGet = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                soloGet.add(() -> get(v, "/api/v1/misiones"));
            }
            assertThat(aLaVez(soloGet)).allSatisfy(x -> assertThat(x.statusCode()).as(x.body()).isEqualTo(200));
        }
    }

    // ---------------------------------------------------------------- QA-03

    @Test
    void qa03CuidadosConLecturasYSeleccionarSimultaneosNoSePierden() throws Exception {
        int u = usuarioConSesion();
        int planta = plantaDigitalizada(u, "Riego" + UUID.randomUUID().toString().substring(0, 6));
        int otra = plantaDigitalizada(u, "Otra" + UUID.randomUUID().toString().substring(0, 6));
        jdbc.update("UPDATE plantas_usuario SET hidratacion_porcentaje = 0, sol_porcentaje = 50, salud_porcentaje = 50 WHERE id_planta_usuario = ?", planta);

        List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tareas.add(() -> post(u, "/api/v1/plantas/" + planta + "/cuidados", "{\"accion\":\"REGAR\"}"));
            tareas.add(() -> get(u, "/api/v1/plantas"));
            tareas.add(() -> get(u, "/api/v1/plantas/activa"));
        }
        for (int i = 0; i < 3; i++) {
            int elegida = i % 2 == 0 ? planta : otra;
            tareas.add(() -> post(u, "/api/v1/plantas/" + elegida + "/seleccionar", ""));
        }
        assertThat(aLaVez(tareas)).allSatisfy(x -> assertThat(x.statusCode()).as(x.body()).isIn(200, 204));

        assertThat(jdbc.queryForObject("SELECT hidratacion_porcentaje FROM plantas_usuario WHERE id_planta_usuario = ?", Integer.class, planta))
                .isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT riegos_total FROM usuarios WHERE id_usuario = ?", Integer.class, u)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT crecimiento FROM plantas_usuario WHERE id_planta_usuario = ?", Integer.class, planta))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plantas_usuario WHERE id_usuario = ? AND es_activa", Integer.class, u))
                .isEqualTo(1);
    }

    // ---------------------------------------------------------------- QA-04

    @Test
    void qa04ReDigitalizarNoReiniciaNoReviveYConservaElPlazo() throws Exception {
        int u = usuarioConSesion();
        String especie = "Rosa" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int planta = plantaDigitalizada(u, especie);
        assertThat(put(u, "/api/v1/plantas/" + planta + "/apodo", "{\"apodo\":\"Mi Rosita\"}").statusCode()).isEqualTo(200);
        jdbc.update("UPDATE plantas_usuario SET dias_cuidados = 4, crecimiento = 4, etapa_crecimiento = 'BROTE', salud_porcentaje = 40, "
                + "hidratacion_porcentaje = 33, ultimo_dia_abono_tienda = CURRENT_DATE, recuperacion_hasta = now() + interval '3 hours', "
                + "golpe_tipo = 'HUAYCO' WHERE id_planta_usuario = ?", planta);
        int nuevoEscaneo = escaneoId(escanear(u, "sp_" + especie + ".png")); // el escaneo si paga (ESCANEO_REPETIDO)
        int xpAntes = jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u);

        JsonNode otraVez = json.readTree(post(u, "/api/v1/plantas", "{\"idEscaneo\":" + nuevoEscaneo + "}").body());

        assertThat(otraVez.get("id").asInt()).isEqualTo(planta);
        assertThat(otraVez.get("yaEnJardin").asBoolean()).isTrue();
        assertThat(otraVez.get("name").asText()).isEqualTo("Mi Rosita");
        assertThat(otraVez.get("diasCuidados").asInt()).isEqualTo(4);
        assertThat(otraVez.get("enRecuperacion").asBoolean()).isTrue();
        Map<String, Object> fila = jdbc.queryForMap("SELECT apodo, dias_cuidados, crecimiento, etapa_crecimiento, salud_porcentaje, "
                + "hidratacion_porcentaje, ultimo_dia_abono_tienda IS NOT NULL AS abono, recuperacion_hasta IS NOT NULL AS plazo "
                + "FROM plantas_usuario WHERE id_planta_usuario = ?", planta);
        assertThat(fila).containsEntry("apodo", "Mi Rosita").containsEntry("dias_cuidados", 4).containsEntry("crecimiento", 4)
                .containsEntry("etapa_crecimiento", "BROTE").containsEntry("salud_porcentaje", 40)
                .containsEntry("hidratacion_porcentaje", 33).containsEntry("abono", true).containsEntry("plazo", true);
        assertThat(jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u))
                .as("re-digitalizar no paga").isEqualTo(xpAntes);
        assertThat(jdbc.queryForObject("SELECT foto_url FROM plantas_usuario WHERE id_planta_usuario = ?", String.class, planta))
                .as("la foto si se actualiza").isEqualTo(jdbc.queryForObject("SELECT imagen_foto_url FROM escaneos_historial WHERE id_escaneo = ?",
                        String.class, nuevoEscaneo));

        // Muerta: sigue muerta (revivir cuesta hojas)
        jdbc.update("UPDATE plantas_usuario SET etapa_crecimiento = 'MUERTA', salud_porcentaje = 0, recuperacion_hasta = NULL "
                + "WHERE id_planta_usuario = ?", planta);
        JsonNode muerta = json.readTree(digitalizarEscaneo(u, especie).body());
        assertThat(muerta.get("fase").asText()).isEqualTo("MUERTA");
        assertThat(jdbc.queryForObject("SELECT salud_porcentaje FROM plantas_usuario WHERE id_planta_usuario = ?", Integer.class, planta)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plantas_usuario WHERE id_usuario = ?", Integer.class, u)).isEqualTo(1);
    }

    // ---------------------------------------------------------------- QA-05

    @Test
    void qa05NoSeDigitalizaUnaEspecieQueNoEscaneoNiUnaDespublicada() throws Exception {
        int u = usuarioConSesion();
        int ajena = jdbc.queryForObject("INSERT INTO plantas_catalogo (nombre_comun, nombre_cientifico, frecuencia_riego_dias, frecuencia_sol_horas, "
                + "sprite_base_url) VALUES ('Ajena', ?, 3, 4, 'x') RETURNING id_catalogo", Integer.class, "Ajena " + UUID.randomUUID());
        assertThat(post(u, "/api/v1/plantas", "{\"idCatalogo\":" + ajena + "}").statusCode()).isEqualTo(404);

        String especie = "Oculta" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int escaneo = escaneoId(escanear(u, "sp_" + especie + ".png"));
        int cat = jdbc.queryForObject("SELECT id_catalogo FROM escaneos_historial WHERE id_escaneo = ?", Integer.class, escaneo);
        jdbc.update("UPDATE plantas_catalogo SET publicada = FALSE WHERE id_catalogo = ?", cat);
        assertThat(post(u, "/api/v1/plantas", "{\"idCatalogo\":" + cat + "}").statusCode()).isEqualTo(404);
        assertThat(post(u, "/api/v1/plantas", "{\"idEscaneo\":" + escaneo + "}").statusCode()).isEqualTo(404);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plantas_usuario WHERE id_usuario = ?", Integer.class, u)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historial_progreso h JOIN reglas_progreso g ON g.id_regla = h.id_regla "
                + "WHERE h.id_usuario = ? AND g.motivo = 'DIGITALIZAR'", Integer.class, u)).isZero();
    }

    // ---------------------------------------------------------------- QA-06

    @Test
    void qa06ActivarNoSubeDeFaseNiReviveUnaPlantaMuerta() throws Exception {
        int u = usuarioConSesion();
        int planta = plantaDigitalizada(u, "Activar" + UUID.randomUUID().toString().substring(0, 6));
        post(u, "/api/v1/plantas/" + planta + "/activar", "");
        post(u, "/api/v1/plantas/" + planta + "/activar", "");
        assertThat(jdbc.queryForObject("SELECT etapa_crecimiento FROM plantas_usuario WHERE id_planta_usuario = ?", String.class, planta))
                .isEqualTo("SEMILLA");

        jdbc.update("UPDATE plantas_usuario SET etapa_crecimiento = 'MUERTA', salud_porcentaje = 0 WHERE id_planta_usuario = ?", planta);
        post(u, "/api/v1/plantas/" + planta + "/activar", "");
        assertThat(jdbc.queryForObject("SELECT etapa_crecimiento FROM plantas_usuario WHERE id_planta_usuario = ?", String.class, planta))
                .isEqualTo("MUERTA");
        assertThat(post(u, "/api/v1/plantas/" + planta + "/cuidados", "{\"accion\":\"ABONAR\"}").statusCode()).isEqualTo(409);
    }

    // ---------------------------------------------------------------- QA-12

    @Test
    void qa12LaAparienciaYLosDiasLosDecideElEscaneoNoElCliente() throws Exception {
        int u = usuarioConSesion();
        String especie = "Forma" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int escaneo = escaneoId(escanear(u, "sp_" + especie + ".png"));
        String enorme = "x".repeat(2_000_000);
        HttpResponse<String> r = post(u, "/api/v1/plantas", "{\"idEscaneo\":" + escaneo
                + ",\"apariencia\":{\"forma\":\"suculenta\",\"relleno\":\"" + enorme + "\"}}");
        assertThat(r.statusCode()).isEqualTo(200);
        JsonNode planta = json.readTree(r.body());
        assertThat(planta.get("diasTotales").asInt()).isEqualTo(7); // la IA dijo "monstera": mediana
        assertThat(planta.get("apariencia").get("forma").asText()).isEqualTo("monstera");
        assertThat(jdbc.queryForObject("SELECT length(apariencia) FROM plantas_usuario WHERE id_planta_usuario = ?", Integer.class,
                planta.get("id").asInt())).isLessThan(300);
    }

    // ---------------------------------------------------------------- QA-16

    @Test
    void qa16NoSeCompraAbonoParaUnaPlantaDelBaul() throws Exception {
        int u = usuarioConSesion();
        int planta = plantaDigitalizada(u, "Baul" + UUID.randomUUID().toString().substring(0, 6));
        jdbc.update("UPDATE usuarios SET monedas = 500 WHERE id_usuario = ?", u);
        assertThat(post(u, "/api/v1/plantas/" + planta + "/baul", "").statusCode()).isEqualTo(200);
        int antes = jdbc.queryForObject("SELECT monedas FROM usuarios WHERE id_usuario = ?", Integer.class, u);

        assertThat(post(u, "/api/v1/plantas/" + planta + "/abono-tienda", "").statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT monedas FROM usuarios WHERE id_usuario = ?", Integer.class, u)).isEqualTo(antes);
    }

    @Autowired private ResultadoDesastreService resultados;

    @Test
    void qa03ElCierreDeUnEventoConCuidadosYSeleccionarALaVezNoDaDeadlock() throws Exception {
        List<int[]> plantas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int u = usuarioConSesion();
            for (int j = 0; j < 3; j++) {
                plantas.add(new int[] {u, plantaDigitalizada(u, "Cierre" + UUID.randomUUID().toString().replace("-", "").substring(0, 6))});
            }
        }
        int tipo = jdbc.queryForObject("SELECT id_tipo_desastre FROM tipos_desastre ORDER BY 1 LIMIT 1", Integer.class);
        long fin = System.currentTimeMillis() + 8000;
        List<Callable<List<Integer>>> tareas = new ArrayList<>();
        tareas.add(() -> {
            List<Integer> errores = new ArrayList<>();
            while (System.currentTimeMillis() < fin) {
                jdbc.update("INSERT INTO eventos_desastre (id_tipo_desastre, mensaje_notificacion, fecha_inicio, fecha_fin, estado, severidad, procesado) "
                        + "VALUES (?, 'qa cierre', now() - interval '1 hour', now(), 'FINALIZADO', 'MODERADO', FALSE)", tipo);
                try {
                    resultados.procesarPendientes(java.time.LocalDateTime.now());
                } catch (RuntimeException e) {
                    errores.add(-1);
                }
            }
            return errores;
        });
        for (int h = 0; h < 10; h++) {
            int n0 = h;
            tareas.add(() -> {
                List<Integer> codigos = new ArrayList<>();
                int n = n0;
                while (System.currentTimeMillis() < fin) {
                    int[] up = plantas.get(n++ % plantas.size());
                    jdbc.update("UPDATE plantas_usuario SET hidratacion_porcentaje = 0, salud_porcentaje = 60, recuperacion_hasta = NULL "
                            + "WHERE id_planta_usuario = ?", up[1]);
                    HttpResponse<String> r = n % 4 == 0 ? post(up[0], "/api/v1/plantas/" + up[1] + "/seleccionar", "")
                            : post(up[0], "/api/v1/plantas/" + up[1] + "/cuidados", "{\"accion\":\"REGAR\"}");
                    if (r.statusCode() >= 500) {
                        codigos.add(r.statusCode());
                    }
                }
                return codigos;
            });
        }
        assertThat(aLaVez(tareas)).allSatisfy(errores -> assertThat(errores).isEmpty());
    }

    // ---------------------------------------------------------------- segunda vuelta (23/11)

    @Test
    void qa34CancelarUnEventoConUsuariosActivosNoDaDeadlockYQuitaLaPrevencion() throws Exception {
        List<int[]> plantas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int u = usuarioConSesion();
            for (int j = 0; j < 3; j++) {
                plantas.add(new int[] {u, plantaDigitalizada(u, "Cancel" + UUID.randomUUID().toString().replace("-", "").substring(0, 6))});
            }
        }
        int tipo = jdbc.queryForObject("SELECT id_tipo_desastre FROM tipos_desastre ORDER BY 1 LIMIT 1", Integer.class);
        long fin = System.currentTimeMillis() + 6000;
        List<Integer> eventos = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Callable<List<Integer>>> tareas = new ArrayList<>();
        tareas.add(() -> {
            List<Integer> errores = new ArrayList<>();
            while (System.currentTimeMillis() < fin) {
                // Lo que deja el panel al cancelar: FINALIZADO, sin procesar y fecha_inicio = fecha_fin
                for (int[] up : plantas) {
                    jdbc.update("UPDATE plantas_usuario SET esta_refugiada = TRUE, refugiada_desde = now() WHERE id_planta_usuario = ?", up[1]);
                }
                eventos.add(jdbc.queryForObject("INSERT INTO eventos_desastre (id_tipo_desastre, mensaje_notificacion, fecha_inicio, fecha_fin, "
                        + "estado, severidad, procesado) VALUES (?, 'qa cancelado', now(), now(), 'FINALIZADO', 'CRITICO', FALSE) RETURNING id_desastre",
                        Integer.class, tipo));
                try {
                    resultados.procesarPendientes(java.time.LocalDateTime.now());
                } catch (RuntimeException e) {
                    errores.add(-1);
                }
            }
            return errores;
        });
        for (int h = 0; h < 10; h++) {
            int n0 = h;
            tareas.add(() -> {
                List<Integer> codigos = new ArrayList<>();
                int n = n0;
                while (System.currentTimeMillis() < fin) {
                    int[] up = plantas.get(n++ % plantas.size());
                    HttpResponse<String> r = switch (n % 3) {
                        case 0 -> get(up[0], "/api/v1/plantas");
                        case 1 -> post(up[0], "/api/v1/plantas/" + up[1] + "/seleccionar", "");
                        default -> post(up[0], "/api/v1/plantas/" + up[1] + "/cuidados", "{\"accion\":\"REGAR\"}");
                    };
                    if (r.statusCode() >= 500) {
                        codigos.add(r.statusCode());
                    }
                }
                return codigos;
            });
        }
        assertThat(aLaVez(tareas)).allSatisfy(errores -> assertThat(errores).isEmpty());
        resultados.procesarPendientes(java.time.LocalDateTime.now());

        String ids = eventos.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eventos_desastre WHERE id_desastre IN (" + ids + ") AND NOT procesado",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registros_desastre_usuario WHERE id_desastre IN (" + ids + ")",
                Integer.class)).as("cancelar no dana ni paga").isZero();
        for (int[] up : plantas) {
            assertThat(jdbc.queryForObject("SELECT esta_refugiada FROM plantas_usuario WHERE id_planta_usuario = ?", Boolean.class, up[1]))
                    .as("la prevencion se quita").isFalse();
            assertThat(jdbc.queryForObject("SELECT recuperacion_hasta IS NULL FROM plantas_usuario WHERE id_planta_usuario = ?",
                    Boolean.class, up[1])).isTrue();
        }
    }

    @Test
    void qa35UnaRecompensaDeMisionEnormeNoTumbaLosEscaneos() throws Exception {
        List<java.util.Map<String, Object>> antes = jdbc.queryForList(
                "SELECT id_mision, recompensa_monedas, recompensa_xp FROM misiones WHERE tipo = 'ESCANEO' AND activa");
        assertThat(antes).as("hace falta una mision ESCANEO activa").isNotEmpty();
        try {
            jdbc.update("UPDATE misiones SET recompensa_monedas = 2147483647, recompensa_xp = 2147483647 WHERE tipo = 'ESCANEO' AND activa");
            for (int i = 0; i < 3; i++) {
                int u = usuarioConSesion();
                jdbc.update("UPDATE usuarios SET monedas = 2147483000 WHERE id_usuario = ?", u);
                List<Integer> codigos = new ArrayList<>();
                for (int k = 0; k < 4; k++) { // la meta de la mision puede ser de varios escaneos
                    codigos.add(escanear(u, "sp_Desborde" + UUID.randomUUID().toString().replace("-", "").substring(0, 6) + ".png").statusCode());
                }
                assertThat(codigos).containsOnly(200);
                assertThat(jdbc.queryForObject("SELECT monedas FROM usuarios WHERE id_usuario = ?", Integer.class, u)).isPositive();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM escaneos_historial WHERE id_usuario = ?", Integer.class, u)).isEqualTo(4);
            }
        } finally {
            for (java.util.Map<String, Object> m : antes) {
                jdbc.update("UPDATE misiones SET recompensa_monedas = ?, recompensa_xp = ? WHERE id_mision = ?",
                        m.get("recompensa_monedas"), m.get("recompensa_xp"), m.get("id_mision"));
            }
        }
    }

    @Test
    void qa41Qa42MensajesDeRegistroYEspecieDespublicada() throws Exception {
        HttpResponse<String> largo = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/auth/registro"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"nombre\":\"" + "a".repeat(300) + "\",\"email\":\"largo-"
                        + UUID.randomUUID().toString().substring(0, 6) + "@plantia.test\",\"password\":\"12345678\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(largo.statusCode()).isEqualTo(400);
        assertThat(json.readTree(largo.body()).get("error").asText()).contains("entre 2 y 100");

        int u = usuarioConSesion();
        String especie = "Retirada" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int planta = plantaDigitalizada(u, especie);
        int cat = jdbc.queryForObject("SELECT id_catalogo FROM plantas_usuario WHERE id_planta_usuario = ?", Integer.class, planta);
        jdbc.update("UPDATE plantas_catalogo SET publicada = FALSE WHERE id_catalogo = ?", cat);
        // La que ya estaba en el jardin: digitalizar por idCatalogo responde la misma planta, no 404
        HttpResponse<String> otraVez = post(u, "/api/v1/plantas", "{\"idCatalogo\":" + cat + "}");
        assertThat(otraVez.statusCode()).as(otraVez.body()).isEqualTo(200);
        assertThat(json.readTree(otraVez.body()).get("yaEnJardin").asBoolean()).isTrue();
        // Un escaneo nuevo de la especie retirada: no identificada, sin premio
        int xp = jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u);
        JsonNode r = json.readTree(escanear(u, "sp_" + especie + ".png").body());
        assertThat(r.get("identificado").asBoolean()).isFalse();
        assertThat(r.get("idCatalogo").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u)).isEqualTo(xp);
    }

    @Test
    void qa45ElCierreDeUnEventoGlobalNoDejaEsperandoAUnUsuarioHastaElFinal() throws Exception {
        // 300 usuarios x 3 plantas que el evento global golpea, mas un usuario que cuida mientras se procesa
        String lote = UUID.randomUUID().toString().substring(0, 6);
        jdbc.update("INSERT INTO usuarios (nombre, email, password_hash, monedas, experiencia_xp, nivel, racha_dias) "
                + "SELECT 'Carga', 'carga-' || ? || '-' || g || '@plantia.test', 'x', 0, 0, 1, 0 FROM generate_series(1, 300) g", lote);
        int cat = jdbc.queryForObject("INSERT INTO plantas_catalogo (nombre_comun, nombre_cientifico, frecuencia_riego_dias, frecuencia_sol_horas, "
                + "sprite_base_url) VALUES ('Carga', ?, 3, 4, 'x') RETURNING id_catalogo", Integer.class, "Carga " + lote);
        jdbc.update("INSERT INTO plantas_usuario (id_usuario, id_catalogo, apodo, salud_porcentaje, hidratacion_porcentaje, sol_porcentaje, "
                + "etapa_crecimiento, fecha_plantado, updated_at) SELECT u.id_usuario, ?, 'c', 90, 60, 70, 'SEMILLA', now(), now() "
                + "FROM usuarios u CROSS JOIN generate_series(1, 3) WHERE u.email LIKE ?", cat, "carga-" + lote + "-%");
        // Se mide al PRIMER usuario que procesa el cierre (el de menor id con plantas): con una sola transaccion para todo
        // el evento quedaba bloqueado desde el principio hasta el final
        int u = jdbc.queryForObject("SELECT MIN(id_usuario) FROM plantas_usuario", Integer.class);
        jdbc.update("UPDATE usuarios SET session_token = ?, session_expira = now() + interval '1 day' WHERE id_usuario = ?",
                com.plantia.util.TokenSesion.hash("token-" + u), u);
        int planta = jdbc.queryForObject("SELECT MIN(id_planta_usuario) FROM plantas_usuario WHERE id_usuario = ?", Integer.class, u);
        jdbc.update("UPDATE plantas_usuario SET etapa_crecimiento = 'SEMILLA', salud_porcentaje = 90, en_baul = FALSE, "
                + "recuperacion_hasta = NULL WHERE id_planta_usuario = ?", planta);
        int tipo = jdbc.queryForObject("SELECT id_tipo_desastre FROM tipos_desastre ORDER BY 1 LIMIT 1", Integer.class);
        jdbc.update("INSERT INTO eventos_desastre (id_tipo_desastre, mensaje_notificacion, fecha_inicio, fecha_fin, estado, severidad, procesado) "
                + "VALUES (?, 'qa global', now() - interval '1 hour', now(), 'FINALIZADO', 'BAJO', FALSE)", tipo);

        long[] cierre = new long[1];
        Thread hilo = new Thread(() -> {
            long t = System.nanoTime();
            resultados.procesarPendientes(java.time.LocalDateTime.now());
            cierre[0] = (System.nanoTime() - t) / 1_000_000;
        });
        hilo.start();
        long peor = 0;
        int cuidados = 0;
        while (hilo.isAlive()) {
            jdbc.update("UPDATE plantas_usuario SET hidratacion_porcentaje = 0, salud_porcentaje = 90, recuperacion_hasta = NULL "
                    + "WHERE id_planta_usuario = ?", planta);
            long t = System.nanoTime();
            assertThat(post(u, "/api/v1/plantas/" + planta + "/cuidados", "{\"accion\":\"REGAR\"}").statusCode()).isEqualTo(200);
            peor = Math.max(peor, (System.nanoTime() - t) / 1_000_000);
            cuidados++;
        }
        hilo.join();
        System.out.printf("QA-45: cierre global de %d plantas en %d ms; %d cuidados de otro usuario a la vez, el mas lento %d ms%n",
                jdbc.queryForObject("SELECT COUNT(*) FROM plantas_usuario", Integer.class), cierre[0], cuidados, peor);
        assertThat(cuidados).isPositive();
        // Con una sola transaccion para todo el evento, el cuidado esperaba el cierre entero
        assertThat(peor).isLessThan(Math.max(1500, cierre[0] / 2));
    }

    // ---------------------------------------------------------------- medias y bajas por HTTP

    @Test
    void qa24Qa30ErroresConCuerpoEnEspanolEIdsEnteros() throws Exception {
        int u = usuarioConSesion();
        int planta = plantaDigitalizada(u, "Error" + UUID.randomUUID().toString().substring(0, 6));
        for (String cuerpo : List.of("{}", "{\"accion\":null}")) {
            HttpResponse<String> r = post(u, "/api/v1/plantas/" + planta + "/cuidados", cuerpo);
            assertThat(r.statusCode()).isEqualTo(400);
            assertThat(json.readTree(r.body()).get("error").asText()).contains("accion");
        }
        for (String cuerpo : List.of("{\"idCatalogo\":\"1\"}", "{\"idCatalogo\":1.9}", "{\"idCatalogo\":1099511627776}")) {
            HttpResponse<String> r = post(u, "/api/v1/plantas", cuerpo);
            assertThat(r.statusCode()).as(cuerpo).isEqualTo(400);
            assertThat(json.readTree(r.body()).has("error")).isTrue();
        }
        HttpResponse<String> registro = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/auth/registro"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"nombre\":\"Ana\",\"email\":\"no-es-correo\",\"password\":\"12345678\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(registro.statusCode()).isEqualTo(400);
        assertThat(json.readTree(registro.body()).get("error").asText()).isEqualTo("Correo inválido");
    }

    @Test
    void qa25Qa26ColeccionIdempotenteYApodoSinEtiquetas() throws Exception {
        int u = usuarioConSesion();
        int escaneo = escaneoId(escanear(u, "sp_Col" + UUID.randomUUID().toString().replace("-", "").substring(0, 6) + ".png"));
        for (int i = 0; i < 3; i++) {
            assertThat(post(u, "/api/v1/coleccion", "{\"idEscaneo\":" + escaneo + "}").statusCode()).isIn(200, 201);
        }
        assertThat(jdbc.queryForObject("SELECT veces_vista FROM coleccion_usuario WHERE id_usuario = ?", Integer.class, u)).isEqualTo(1);

        int planta = json.readTree(post(u, "/api/v1/plantas", "{\"idEscaneo\":" + escaneo + "}").body()).get("id").asInt();
        put(u, "/api/v1/plantas/" + planta + "/apodo", "{\"apodo\":\"<img src=x onerror=alert(1)>Rosa\"}");
        assertThat(jdbc.queryForObject("SELECT apodo FROM plantas_usuario WHERE id_planta_usuario = ?", String.class, planta)).isEqualTo("Rosa");
        put(u, "/api/v1/usuarios/me/nombre", "{\"nombre\":\"<b>Ana</b>\"}");
        assertThat(jdbc.queryForObject("SELECT nombre FROM usuarios WHERE id_usuario = ?", String.class, u)).isEqualTo("Ana");
    }

    @Test
    void qa27TrasCincoClavesMalasElLoginSeFrena() throws Exception {
        String correo = "qa-login-" + UUID.randomUUID().toString().substring(0, 8) + "@plantia.test";
        jdbc.update("INSERT INTO usuarios (nombre, email, password_hash, monedas, experiencia_xp, nivel, racha_dias) "
                + "VALUES ('QA', ?, '$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy', 0, 0, 1, 0)", correo);
        List<Integer> codigos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            codigos.add(http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + correo + "\",\"password\":\"equivocada-" + i + "\"}")).build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        assertThat(codigos).containsExactly(401, 401, 401, 401, 401, 429);
    }

    // ---------------------------------------------------------------- ayudas

    private <T> List<T> aLaVez(List<Callable<T>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<T>> futuros = new ArrayList<>();
        for (Callable<T> t : tareas) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return t.call();
            }));
        }
        salida.countDown();
        List<T> resultados = Collections.synchronizedList(new ArrayList<>());
        for (Future<T> f : futuros) {
            resultados.add(f.get(90, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return resultados;
    }

    private int usuarioConSesion() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        int u = jdbc.queryForObject("INSERT INTO usuarios (nombre, email, password_hash, monedas, experiencia_xp, nivel, racha_dias) "
                + "VALUES (?, ?, 'x', 0, 0, 1, 0) RETURNING id_usuario", Integer.class, "QA " + sufijo, "qa-" + sufijo + "@plantia.test");
        jdbc.update("UPDATE usuarios SET session_token = ?, session_expira = now() + interval '1 day' WHERE id_usuario = ?",
                com.plantia.util.TokenSesion.hash("token-" + u), u);
        return u;
    }

    private int plantaDigitalizada(int u, String especie) throws Exception {
        HttpResponse<String> r = digitalizarEscaneo(u, especie);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        return json.readTree(r.body()).get("id").asInt();
    }

    private HttpResponse<String> digitalizarEscaneo(int u, String especie) throws Exception {
        int escaneo = escaneoId(escanear(u, "sp_" + especie + ".png"));
        return post(u, "/api/v1/plantas", "{\"idEscaneo\":" + escaneo + "}");
    }

    private int escaneoId(HttpResponse<String> r) throws Exception {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        return Integer.parseInt(json.readTree(r.body()).get("idEscaneo").asText());
    }

    private HttpResponse<String> escanear(int u, String archivo) throws Exception {
        String limite = "----qa" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        cuerpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"" + archivo + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.write(png());
        cuerpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return http.send(peticion(u, "/api/v1/escaneos").header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] png() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private HttpResponse<String> post(int u, String ruta, String cuerpo) throws Exception {
        return http.send(peticion(u, ruta).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(int u, String ruta, String cuerpo) throws Exception {
        return http.send(peticion(u, ruta).header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(int u, String ruta) throws Exception {
        return http.send(peticion(u, ruta).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder peticion(int u, String ruta) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .timeout(Duration.ofSeconds(60))
                .header("X-User-Id", String.valueOf(u))
                .header("X-Session-Token", "token-" + u);
    }

    private static String envO(String nombre, String defecto) {
        String v = System.getenv(nombre);
        return v == null || v.isBlank() ? defecto : v;
    }

    /** Como fake_ia.py del tester: especie segun el nombre del archivo, forma "monstera" (mediana, 7 dias). */
    private static HttpServer iniciarIaFalsa() throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Pattern archivo = Pattern.compile("filename=\"([^\"]*)\"");
        Pattern especie = Pattern.compile("sp_([A-Za-z0-9]+)");
        s.createContext("/api/ia/identificar", ex -> {
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            Matcher a = archivo.matcher(cuerpo);
            String nombre = a.find() ? a.group(1) : "scan.png";
            Matcher e = especie.matcher(nombre);
            String sp = e.find() ? e.group(1) : "Monstera";
            String respuesta = "{\"nombreComun\":\"" + sp + "\",\"nombreCientifico\":\"" + sp + " qaensis\",\"confianza\":92.0,"
                    + "\"cuidados\":[{\"tipo\":\"RIEGO\",\"titulo\":\"Riego\",\"contenido\":\"Semanal\"},"
                    + "{\"tipo\":\"LUZ\",\"titulo\":\"Luz\",\"contenido\":\"Indirecta\"}],\"datosRaw\":\"{}\","
                    + "\"apariencia\":{\"forma\":\"monstera\",\"colorHoja\":\"#2f7d4a\"}}";
            byte[] b = respuesta.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        s.setExecutor(Executors.newFixedThreadPool(16));
        s.start();
        return s;
    }
}
