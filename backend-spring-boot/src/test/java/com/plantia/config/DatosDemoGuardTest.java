package com.plantia.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** QA-09: en produccion las cuentas y el evento de demostracion quedan inofensivos; en desarrollo no se tocan. */
class DatosDemoGuardTest {

    @Test
    void enDesarrolloNoTocaNada() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        new DatosDemoGuard(jdbc, false).run(null);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void enProduccionDeshabilitaLasCuentasDemoYCancelaElEventoSembrado() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        new DatosDemoGuard(jdbc, true).run(null);
        for (String correo : DatosDemoGuard.CUENTAS_DEMO) {
            verify(jdbc).update(org.mockito.ArgumentMatchers.startsWith("UPDATE usuarios SET password_hash"),
                    eq(DatosDemoGuard.CLAVE_INUTILIZABLE), eq(correo), eq(DatosDemoGuard.CLAVE_INUTILIZABLE));
        }
        verify(jdbc, times(1)).update(org.mockito.ArgumentMatchers.startsWith("UPDATE eventos_desastre SET estado = 'FINALIZADO', procesado = TRUE"),
                eq(DatosDemoGuard.MENSAJE_EVENTO_DEMO), eq(DatosDemoGuard.MENSAJE_EVENTO_DEMO_V32));
    }
}
