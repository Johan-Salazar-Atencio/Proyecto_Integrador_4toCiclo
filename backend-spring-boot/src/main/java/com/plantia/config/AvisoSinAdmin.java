package com.plantia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * QA-09 / QA-40: desde V33 la base no trae usuarios de demostracion, asi que una base nueva arranca sin admin y nadie
 * puede entrar al panel. Al arrancar solo se avisa en el log como crearlo; no se crea ni se cambia nada.
 */
@Component
public class AvisoSinAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AvisoSinAdmin.class);

    static final String MENSAJE = "No hay ningun administrador. Crealo con 'python manage.py crear_admin --email <correo>' "
            + "en backend-django (ver GUIA-INICIO.md)";

    private final JdbcTemplate jdbc;

    public AvisoSinAdmin(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!hayAdmin()) {
            log.warn(MENSAJE);
        }
    }

    boolean hayAdmin() {
        Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN'", Integer.class);
        return admins != null && admins > 0;
    }
}
