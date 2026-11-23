package com.plantia.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * QA-09: las migraciones V3, V4, V6 y V9 (ya aplicadas, no se pueden editar) siembran un ADMIN y usuarios de
 * demostracion con clave conocida, y un evento HUAYCO critico de 7 dias. En desarrollo hacen falta; en produccion
 * (PLANTIA_PRODUCCION=true) este arranque los deja inofensivos: las cuentas demo no pueden iniciar sesion (clave
 * inutilizable y sesiones cerradas; no se borra nada, se puede restablecer) y el evento sembrado queda cancelado sin
 * danos. En desarrollo (por defecto) no hace nada.
 */
@Component
public class DatosDemoGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoGuard.class);

    /** Correos que siembran las migraciones de desarrollo. */
    static final List<String> CUENTAS_DEMO = List.of("admin@plantia.test", "johan@plantia.test", "lucia@plantia.test");
    /** Texto del evento que siembra V3 (asi se distingue de uno que lanzo el admin). */
    static final String MENSAJE_EVENTO_DEMO = "Resguarda las macetas de terraza para evitar exceso de lluvia y danos.";
    /** El mismo texto con tilde (V32, QA-61). */
    static final String MENSAJE_EVENTO_DEMO_V32 = "Resguarda las macetas de terraza para evitar exceso de lluvia y daños.";
    /** No es un hash BCrypt: ningun login (Spring ni Django) lo acepta. */
    static final String CLAVE_INUTILIZABLE = "!deshabilitada-en-produccion";

    private final JdbcTemplate jdbc;
    private final boolean produccion;

    public DatosDemoGuard(JdbcTemplate jdbc, @Value("${app.produccion:false}") boolean produccion) {
        this.jdbc = jdbc;
        this.produccion = produccion;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!produccion) {
            return;
        }
        int cuentas = 0;
        for (String correo : CUENTAS_DEMO) {
            cuentas += jdbc.update("UPDATE usuarios SET password_hash = ?, session_token = NULL, session_expira = NULL, "
                    + "admin_session_token = NULL, admin_session_expira = NULL WHERE email = ? AND password_hash <> ?",
                    CLAVE_INUTILIZABLE, correo, CLAVE_INUTILIZABLE);
        }
        int eventos = jdbc.update("UPDATE eventos_desastre SET estado = 'FINALIZADO', procesado = TRUE, updated_at = now() "
                + "WHERE mensaje_notificacion IN (?, ?) AND estado <> 'FINALIZADO'", MENSAJE_EVENTO_DEMO, MENSAJE_EVENTO_DEMO_V32);
        if (cuentas > 0 || eventos > 0) {
            log.warn("Produccion: {} cuenta(s) de demostracion deshabilitadas y {} evento(s) de demostracion cancelados", cuentas, eventos);
        }
        // QA-40: sin admin nadie entra al panel. Se crea a proposito, sin clave fija:
        // python manage.py crear_admin --email ... (clave por PLANTIA_ADMIN_PASSWORD o por teclado)
        Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND password_hash <> ?",
                Integer.class, CLAVE_INUTILIZABLE);
        if (admins == null || admins == 0) {
            log.warn("Produccion: no hay ningun administrador con clave valida. Crealo con "
                    + "'python manage.py crear_admin --email <correo>' en backend-django (ver GUIA-INICIO.md)");
        }
    }
}
