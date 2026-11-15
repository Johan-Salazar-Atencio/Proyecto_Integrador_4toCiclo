package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El nombre del archivo lo elige el cliente: no puede romper el multipart que se envia a Django. */
class IaClientTest {

    @Test
    void nombreConComillasYSaltosNoInyectaCabeceras() {
        String nombre = IaClient.nombreSeguro("a\"\r\nX-Malo: 1\\.jpg");
        assertThat(nombre).doesNotContain("\"", "\r", "\n", "\\").isEqualTo("a___X-Malo: 1_.jpg");
    }

    @Test
    void sinNombreUsaUnoPorDefecto() {
        assertThat(IaClient.nombreSeguro(null)).isEqualTo("scan.jpg");
        assertThat(IaClient.nombreSeguro("monstera.png")).isEqualTo("monstera.png");
    }
}
