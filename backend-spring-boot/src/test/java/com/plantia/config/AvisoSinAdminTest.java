package com.plantia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** QA-09 / QA-40: sin datos demo, al arrancar solo se avisa si falta el admin; nunca se escribe en la base. */
class AvisoSinAdminTest {

    @Test
    void sinAdminLoDetectaYNoEscribeNada() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        AvisoSinAdmin aviso = new AvisoSinAdmin(jdbc);
        assertThat(aviso.hayAdmin()).isFalse();
        aviso.run(null);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(jdbc, never()).update(anyString());
    }

    @Test
    void conAdminNoHaceFaltaAvisar() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        assertThat(new AvisoSinAdmin(jdbc).hayAdmin()).isTrue();
    }

    @Test
    void elAvisoDiceComoCrearElAdmin() {
        assertThat(AvisoSinAdmin.MENSAJE).contains("python manage.py crear_admin --email");
    }
}
