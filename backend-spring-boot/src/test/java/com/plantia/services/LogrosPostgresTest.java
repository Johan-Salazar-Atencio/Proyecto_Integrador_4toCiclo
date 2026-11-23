package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

import com.plantia.dto.LogroDesbloqueadoDto;
import com.plantia.dto.LogroDto;
import com.plantia.dto.ProgresoDto;

/**
 * Logros contra un Postgres REAL (Flyway aplica V1..V30 desde cero). No corre en {@code mvn test} normal:
 * solo si se define PLANTIA_TEST_DB_URL, y se niega a usar una base cuyo nombre no diga "prueba" o "test"
 * (nunca la base de desarrollo plantia). Ejemplo:
 * <pre>
 * PLANTIA_TEST_DB_URL=jdbc:postgresql://localhost:55433/plantia_prueba?stringtype=unspecified mvn test -Dtest=LogrosPostgresTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "PLANTIA_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.eventos.intervalo-ms=3600000", // el scheduler de eventos no se mete en las pruebas
        "spring.flyway.locations=filesystem:../db/migration",
        // Pool como en produccion (10) pero esperando 5 s en vez de 30: si vuelve el agotamiento, se ve rapido
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.datasource.hikari.connection-timeout=5000"
})
class LogrosPostgresTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Autowired private LogroService logroService;
    @Autowired private ProgresoService progresoService;
    @Autowired private JdbcTemplate jdbc;
    @org.springframework.boot.test.web.server.LocalServerPort private int puerto;
    private final java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
            .executor(Executors.newFixedThreadPool(40)).build();

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

    // ---------------------------------------------------------------- migracion

    @Test
    void v30QuedoAplicadaConContadoresYFkRestrict() {
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '30'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'usuarios' "
                + "AND column_name IN ('riegos_total', 'abonos_total', 'luz_total')", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT confdeltype FROM pg_constraint WHERE conname = 'logros_usuario_id_logro_fkey'",
                String.class)).isEqualTo("r");

        int u = usuarioNuevo();
        int logro = idLogro("PRIMERA_PLANTA");
        jdbc.update("INSERT INTO logros_usuario (id_usuario, id_logro, progreso, notificado) VALUES (?, ?, 0, FALSE)", u, logro);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM logros WHERE id_logro = ?", logro))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- 1. carrera real

    @Test
    void carreraRealDeEvaluacionesYAccionesPagaCadaLogroUnaSolaVez() throws Exception {
        int u = usuarioNuevo();
        escaneo(u, LocalDateTime.now().withHour(12));
        // Filas ya creadas con desbloqueado_en nulo: el caso en que antes pagaban todos los que llegaban a la vez
        jdbc.update("INSERT INTO logros_usuario (id_usuario, id_logro, progreso, notificado) "
                + "SELECT ?, id_logro, 0, FALSE FROM logros WHERE activo", u);

        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<List<LogroDesbloqueadoDto>>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            int n = i;
            resultados.add(pool.submit(() -> {
                salida.await();
                return switch (n % 3) {
                    case 0 -> logroService.evaluarSeguro(u, LocalDateTime.now());            // scheduler / compra
                    case 1 -> { logroService.listar(u, LocalDateTime.now()); yield List.of(); } // GET /logros
                    default -> progresoService.otorgar(u, List.of("ESCANEO"), 0).logrosNuevos(); // accion + tras commit
                };
            }));
        }
        salida.countDown();
        int primeraPlanta = 0;
        for (Future<List<LogroDesbloqueadoDto>> f : resultados) {
            primeraPlanta += (int) f.get(60, TimeUnit.SECONDS).stream().filter(l -> "PRIMERA_PLANTA".equals(l.codigo())).count();
        }
        pool.shutdown();

        assertThat(primeraPlanta).isLessThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT desbloqueado_en IS NOT NULL FROM logros_usuario WHERE id_usuario = ? AND id_logro = ?",
                Boolean.class, u, idLogro("PRIMERA_PLANTA"))).isTrue();
        // Ningun logro pagado dos veces
        assertThat(jdbc.queryForList("SELECT h.id_referencia FROM historial_progreso h JOIN reglas_progreso r ON r.id_regla = h.id_regla "
                + "WHERE r.motivo = 'LOGRO' AND h.id_usuario = ? GROUP BY h.id_referencia HAVING COUNT(*) > 1", Integer.class, u)).isEmpty();
        // Cada logro desbloqueado con premio tiene exactamente un pago
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM logros_usuario lu JOIN logros l ON l.id_logro = lu.id_logro "
                + "WHERE lu.id_usuario = ? AND lu.desbloqueado_en IS NOT NULL AND (l.recompensa_xp > 0 OR l.recompensa_monedas > 0)",
                Integer.class, u))
                .isEqualTo(jdbc.queryForObject("SELECT COUNT(*) FROM historial_progreso h JOIN reglas_progreso r ON r.id_regla = h.id_regla "
                        + "WHERE r.motivo = 'LOGRO' AND h.id_usuario = ?", Integer.class, u));
        // Ningun XP perdido ni duplicado: todo lo ganado esta en el historial
        assertThat(jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u))
                .isEqualTo(jdbc.queryForObject("SELECT COALESCE(SUM(xp_ganado), 0) FROM historial_progreso WHERE id_usuario = ?", Integer.class, u));
    }

    // ---------------------------------------------------------------- 2. cuidados pasado el tope

    @Test
    void losRiegosCuentanAunquePaseElTopeDiario() {
        int u = usuarioNuevo();
        for (int i = 0; i < 7; i++) {
            progresoService.registrarCuidado(u, "REGAR");
            progresoService.otorgar(u, List.of("REGAR"), 0);
        }

        assertThat(jdbc.queryForObject("SELECT riegos_total FROM usuarios WHERE id_usuario = ?", Integer.class, u)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historial_progreso h JOIN reglas_progreso r ON r.id_regla = h.id_regla "
                + "WHERE r.motivo = 'REGAR' AND h.id_usuario = ?", Integer.class, u)).isEqualTo(5); // tope REGAR
        assertThat(desbloqueado(u, "PRIMERA_GOTA")).isTrue();
    }

    // ---------------------------------------------------------------- 3. fallo de BD aislado

    @Test
    void unErrorDeBaseEnLogrosNoTumbaLaAccionNiDejaNadaAMedias() {
        int u = usuarioNuevo();
        escaneo(u, LocalDateTime.now().withHour(12));
        jdbc.execute("CREATE OR REPLACE FUNCTION falla_logros_" + u + "() RETURNS trigger AS $$ BEGIN "
                + "IF NEW.id_usuario = " + u + " THEN RAISE EXCEPTION 'falla de prueba en logros'; END IF; RETURN NEW; END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER falla_logros_" + u + " BEFORE INSERT OR UPDATE ON logros_usuario "
                + "FOR EACH ROW EXECUTE FUNCTION falla_logros_" + u + "()");
        try {
            ProgresoDto p = progresoService.otorgar(u, List.of("ESCANEO"), List.of("ESCANEO"), null);

            assertThat(p.xpGanado()).isPositive();
            assertThat(p.logrosNuevos()).isEmpty();
            assertThat(jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u))
                    .isEqualTo(p.xpGanado()); // la accion principal SI quedo guardada
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM logros_usuario WHERE id_usuario = ?", Integer.class, u)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER falla_logros_" + u + " ON logros_usuario");
            jdbc.execute("DROP FUNCTION falla_logros_" + u + "()");
        }
        // Arreglada la base, la siguiente evaluacion lo desbloquea (una vez)
        assertThat(logroService.evaluarSeguro(u, LocalDateTime.now())).extracting(LogroDesbloqueadoDto::codigo).contains("PRIMERA_PLANTA");
        assertThat(logroService.evaluarSeguro(u, LocalDateTime.now())).isEmpty();
    }

    @Test
    void siFallaElPagoElLogroNoQuedaDesbloqueadoSinPremio() {
        int u = usuarioNuevo();
        escaneo(u, LocalDateTime.now().withHour(12));
        int reglaLogro = jdbc.queryForObject("SELECT id_regla FROM reglas_progreso WHERE motivo = 'LOGRO'", Integer.class);
        jdbc.execute("CREATE OR REPLACE FUNCTION falla_pago_" + u + "() RETURNS trigger AS $$ BEGIN "
                + "IF NEW.id_usuario = " + u + " AND NEW.id_regla = " + reglaLogro + " THEN RAISE EXCEPTION 'falla de prueba en pago'; END IF; "
                + "RETURN NEW; END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER falla_pago_" + u + " BEFORE INSERT ON historial_progreso "
                + "FOR EACH ROW EXECUTE FUNCTION falla_pago_" + u + "()");
        int xpAccion;
        try {
            xpAccion = progresoService.otorgar(u, List.of("ESCANEO"), List.of("ESCANEO"), null).xpGanado();
        } finally {
            jdbc.execute("DROP TRIGGER falla_pago_" + u + " ON historial_progreso");
            jdbc.execute("DROP FUNCTION falla_pago_" + u + "()");
        }

        assertThat(desbloqueado(u, "PRIMERA_PLANTA")).isFalse(); // ni desbloqueado...
        assertThat(jdbc.queryForObject("SELECT experiencia_xp FROM usuarios WHERE id_usuario = ?", Integer.class, u))
                .isEqualTo(xpAccion);                              // ...ni XP del logro sin su fila de historial
    }

    // ---------------------------------------------------------------- 4. secretos

    @Test
    void elListadoNoRevelaElCodigoDeUnSecretoBloqueado() {
        int u = usuarioNuevo();
        List<LogroDto> logros = logroService.listar(u, LocalDateTime.now()).logros();

        assertThat(logros).extracting(LogroDto::codigo).doesNotContain("MADRUGADOR", "NOCTURNO")
                .contains("secreto_" + idLogro("MADRUGADOR"), "secreto_" + idLogro("NOCTURNO"));
    }

    // ---------------------------------------------------------------- 5. zona horaria

    @Test
    void lasFranjasDeEscaneoSeMidenEnHoraDeLima() {
        LocalDate dia = LocalDate.now().minusDays(1);
        int madrugador = usuarioNuevo();
        escaneo(madrugador, enHoraDelServidor(dia, LocalTime.of(5, 30)));
        int tarde = usuarioNuevo();
        escaneo(tarde, enHoraDelServidor(dia, LocalTime.of(22, 30)));
        int nocturno = usuarioNuevo();
        escaneo(nocturno, enHoraDelServidor(dia, LocalTime.of(23, 15)));

        for (int u : new int[] {madrugador, tarde, nocturno}) {
            logroService.evaluarSeguro(u, LocalDateTime.now());
        }

        assertThat(desbloqueado(madrugador, "MADRUGADOR")).isTrue();
        assertThat(desbloqueado(madrugador, "NOCTURNO")).isFalse();
        assertThat(desbloqueado(tarde, "MADRUGADOR")).isFalse();
        assertThat(desbloqueado(tarde, "NOCTURNO")).isFalse();
        assertThat(desbloqueado(nocturno, "NOCTURNO")).isTrue();
    }

    // ---------------------------------------------------------------- marcarVistos

    @Test
    void marcarVistosNoTocaLogrosDeOtroUsuario() {
        int duena = usuarioNuevo();
        int intrusa = usuarioNuevo();
        escaneo(duena, LocalDateTime.now().withHour(12));
        logroService.evaluarSeguro(duena, LocalDateTime.now());
        int logro = idLogro("PRIMERA_PLANTA");

        logroService.marcarVistos(intrusa, List.of(logro));
        assertThat(notificado(duena, logro)).isFalse();

        logroService.marcarVistos(duena, List.of(logro));
        assertThat(notificado(duena, logro)).isTrue();
    }

    // ---------------------------------------------------------------- utilidades

    private LocalDateTime enHoraDelServidor(LocalDate dia, LocalTime horaLima) {
        // fecha_escaneo se guarda con LocalDateTime.now() del servidor: se escribe igual que lo haria EscaneoService
        return ZonedDateTime.of(dia, horaLima, LIMA).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private int usuarioNuevo() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        return jdbc.queryForObject("INSERT INTO usuarios (nombre, email, password_hash, monedas, experiencia_xp, nivel, racha_dias) "
                + "VALUES (?, ?, 'x', 0, 0, 1, 0) RETURNING id_usuario", Integer.class, "Prueba " + sufijo, "prueba-" + sufijo + "@plantia.test");
    }

    private void escaneo(int u, LocalDateTime fecha) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        int catalogo = jdbc.queryForObject("INSERT INTO plantas_catalogo (nombre_comun, nombre_cientifico, frecuencia_riego_dias, "
                + "frecuencia_sol_horas, sprite_base_url) VALUES ('Prueba', ?, 3, 4, 'x') RETURNING id_catalogo", Integer.class, "Planta prueba " + sufijo);
        jdbc.update("INSERT INTO escaneos_historial (id_usuario, id_catalogo, imagen_foto_url, respuesta_ia_raw, fecha_escaneo) "
                + "VALUES (?, ?, 'x', '{}'::jsonb, ?)", u, catalogo, fecha);
    }

    private int idLogro(String codigo) {
        return jdbc.queryForObject("SELECT id_logro FROM logros WHERE codigo = ?", Integer.class, codigo);
    }

    private boolean desbloqueado(int u, String codigo) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM logros_usuario WHERE id_usuario = ? AND id_logro = ? "
                + "AND desbloqueado_en IS NOT NULL)", Boolean.class, u, idLogro(codigo)));
    }

    private boolean notificado(int u, int logro) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT notificado FROM logros_usuario WHERE id_usuario = ? AND id_logro = ?",
                Boolean.class, u, logro));
    }

    // ---------------------------------------------------------------- B1: pool de conexiones (HTTP real)

    @Test
    void quinceAccionesSimultaneasDeUnUsuarioNoCongelanLaApi() throws Exception {
        int u = usuarioConSesion();
        int victima = usuarioConSesion();
        int planta = plantaPara(u);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<long[]>> cuidados = new ArrayList<>();
        List<String> acciones = new ArrayList<>();
        for (String a : List.of("REGAR", "SOL", "ABONAR")) {
            for (int i = 0; i < 5; i++) {
                acciones.add(a);
            }
        }
        List<java.net.http.HttpResponse<String>> respuestas = java.util.Collections.synchronizedList(new ArrayList<>());
        for (String a : acciones) {
            cuidados.add(pool.submit(() -> {
                salida.await();
                long t = System.nanoTime();
                java.net.http.HttpResponse<String> r = post(u, "/api/v1/plantas/" + planta + "/cuidados", "{\"accion\":\"" + a + "\"}");
                respuestas.add(r);
                return new long[] {r.statusCode(), (System.nanoTime() - t) / 1_000_000};
            }));
        }
        Future<long[]> otro = pool.submit(() -> {
            salida.await();
            Thread.sleep(200);
            long t = System.nanoTime();
            int codigo = get(victima, "/api/v1/logros").statusCode();
            return new long[] {codigo, (System.nanoTime() - t) / 1_000_000};
        });
        long inicio = System.nanoTime();
        salida.countDown();
        long peor = 0;
        for (Future<long[]> f : cuidados) {
            long[] r = f.get(60, TimeUnit.SECONDS);
            assertThat(r[0]).as("codigo HTTP de un cuidado").isEqualTo(200);
            peor = Math.max(peor, r[1]);
        }
        long[] v = otro.get(60, TimeUnit.SECONDS);
        long total = (System.nanoTime() - inicio) / 1_000_000;
        pool.shutdown();
        System.out.printf("B1 mismo usuario: 15 cuidados en %d ms (peor %d ms); GET de otro usuario %d -> %d ms%n",
                total, peor, v[0], v[1]);

        assertThat(v[0]).isEqualTo(200);
        assertThat(v[1]).as("ms del GET de otro usuario").isLessThan(3000);
        assertThat(peor).as("ms del cuidado mas lento").isLessThan(4000); // con el pool agotado serian 5000+ y 500
        assertThat(jdbc.queryForList("SELECT riegos_total, luz_total, abonos_total FROM usuarios WHERE id_usuario = ?", u).get(0))
                .containsValues(5, 5, 5);
        // logrosNuevos sigue llegando en la respuesta, y PRIMERA_GOTA en una sola
        long conPrimeraGota = respuestas.stream().filter(r -> r.body().contains("\"PRIMERA_GOTA\"")).count();
        assertThat(conPrimeraGota).isEqualTo(1);
        assertThat(desbloqueado(u, "PRIMERA_GOTA")).isTrue();
        assertThat(jdbc.queryForList("SELECT h.id_referencia FROM historial_progreso h JOIN reglas_progreso r ON r.id_regla = h.id_regla "
                + "WHERE r.motivo = 'LOGRO' AND h.id_usuario = ? GROUP BY h.id_referencia HAVING COUNT(*) > 1", Integer.class, u)).isEmpty();
    }

    @Test
    void muchosUsuariosConVariasAccionesALaVez() throws Exception {
        int usuarios = 10;
        List<int[]> trabajo = new ArrayList<>();
        for (int i = 0; i < usuarios; i++) {
            int u = usuarioConSesion();
            int planta = plantaPara(u);
            for (String a : List.of("REGAR", "SOL", "ABONAR")) {
                trabajo.add(new int[] {u, planta, a.hashCode()});
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(trabajo.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<long[]>> futuros = new ArrayList<>();
        for (int[] t : trabajo) {
            String accion = t[2] == "REGAR".hashCode() ? "REGAR" : t[2] == "SOL".hashCode() ? "SOL" : "ABONAR";
            futuros.add(pool.submit(() -> {
                salida.await();
                long ini = System.nanoTime();
                int codigo = post(t[0], "/api/v1/plantas/" + t[1] + "/cuidados", "{\"accion\":\"" + accion + "\"}").statusCode();
                return new long[] {codigo, (System.nanoTime() - ini) / 1_000_000};
            }));
        }
        long inicio = System.nanoTime();
        salida.countDown();
        long peor = 0;
        for (Future<long[]> f : futuros) {
            long[] r = f.get(60, TimeUnit.SECONDS);
            assertThat(r[0]).isEqualTo(200);
            peor = Math.max(peor, r[1]);
        }
        pool.shutdown();
        System.out.printf("B1 varios usuarios: %d cuidados de %d usuarios en %d ms (peor %d ms)%n",
                trabajo.size(), usuarios, (System.nanoTime() - inicio) / 1_000_000, peor);
        assertThat(peor).isLessThan(4000);
    }

    // ---------------------------------------------------------------- vistos y secretos por HTTP

    @Test
    void vistosAceptaHastaCienIdsYRechazaMas() throws Exception {
        int u = usuarioConSesion();
        String cien = java.util.stream.IntStream.rangeClosed(1, 100).mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(","));
        assertThat(post(u, "/api/v1/logros/vistos", "{\"ids\":[" + cien + "]}").statusCode()).isEqualTo(204);
        assertThat(post(u, "/api/v1/logros/vistos", "{\"ids\":[" + cien + ",101]}").statusCode()).isEqualTo(400);
    }

    @Test
    void secretoBloqueadoNoMuestraNiSuRareza() throws Exception {
        int u = usuarioConSesion();
        String json = get(u, "/api/v1/logros").body();
        com.fasterxml.jackson.databind.JsonNode logros = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("logros");
        int secretos = 0;
        for (com.fasterxml.jackson.databind.JsonNode l : logros) {
            if (l.get("codigo").asText().startsWith("secreto_")) {
                secretos++;
                assertThat(l.get("rareza").asText()).isEqualTo("COMUN");
            }
        }
        assertThat(secretos).isEqualTo(2); // MADRUGADOR y NOCTURNO (ambos RARA en V29)
    }

    private int usuarioConSesion() {
        int u = usuarioNuevo();
        jdbc.update("UPDATE usuarios SET session_token = ?, session_expira = now() + interval '1 day' WHERE id_usuario = ?",
                com.plantia.util.TokenSesion.hash("token-" + u), u);
        return u;
    }

    /** Planta digital creada por la API (digitalizar), con agua, luz y salud bajas para que cada cuidado sea util. */
    private int plantaPara(int u) throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        int catalogo = jdbc.queryForObject("INSERT INTO plantas_catalogo (nombre_comun, nombre_cientifico, frecuencia_riego_dias, "
                + "frecuencia_sol_horas, sprite_base_url) VALUES ('Prueba', ?, 3, 4, 'x') RETURNING id_catalogo", Integer.class, "Planta http " + sufijo);
        // Desde QA-05 solo se digitaliza una especie que el usuario escaneo
        jdbc.update("INSERT INTO escaneos_historial (id_usuario, id_catalogo, imagen_foto_url, respuesta_ia_raw, fecha_escaneo) "
                + "VALUES (?, ?, 'x', '{}'::jsonb, now())", u, catalogo);
        java.net.http.HttpResponse<String> r = post(u, "/api/v1/plantas", "{\"idCatalogo\":" + catalogo + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        int planta = new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.body()).get("id").asInt();
        jdbc.update("UPDATE plantas_usuario SET hidratacion_porcentaje = 10, sol_porcentaje = 10, salud_porcentaje = 50 "
                + "WHERE id_planta_usuario = ?", planta);
        return planta;
    }

    private java.net.http.HttpResponse<String> post(int u, String ruta, String json) throws Exception {
        return http.send(peticion(u, ruta).header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private java.net.http.HttpResponse<String> get(int u, String ruta) throws Exception {
        return http.send(peticion(u, ruta).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private java.net.http.HttpRequest.Builder peticion(int u, String ruta) {
        return java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + puerto + ruta))
                .timeout(java.time.Duration.ofSeconds(60))
                .header("X-User-Id", String.valueOf(u))
                .header("X-Session-Token", "token-" + u);
    }
}
