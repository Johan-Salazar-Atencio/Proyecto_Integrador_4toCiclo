package com.plantia.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenSesionTest {

    @Test
    void enLaBdSoloQuedaElHash() {
        String token = TokenSesion.generar();
        String hash = TokenSesion.hash(token);
        assertThat(token).hasSize(32).isNotEqualTo(hash);
        assertThat(hash).hasSize(64);
        assertThat(TokenSesion.coincide(hash, token)).isTrue();
    }

    @Test
    void tokenIncorrectoOVacioNoCoincide() {
        String hash = TokenSesion.hash(TokenSesion.generar());
        assertThat(TokenSesion.coincide(hash, "otro")).isFalse();
        assertThat(TokenSesion.coincide(hash, "")).isFalse();
        assertThat(TokenSesion.coincide(null, "x")).isFalse();
        // El hash robado de la BD no sirve como token
        assertThat(TokenSesion.coincide(hash, hash)).isFalse();
    }

    @Test
    void cadaTokenEsDistinto() {
        assertThat(TokenSesion.generar()).isNotEqualTo(TokenSesion.generar());
    }
}
