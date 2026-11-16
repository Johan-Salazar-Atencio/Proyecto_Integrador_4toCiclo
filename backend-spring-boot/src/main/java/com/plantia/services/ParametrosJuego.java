package com.plantia.services;

import org.springframework.stereotype.Service;

import com.plantia.models.ParametroJuego;
import com.plantia.repositories.ParametroJuegoRepository;

/** Lee los valores ajustables del juego (tabla parametros_juego); si falta uno usa el de fabrica. */
@Service
public class ParametrosJuego {

    public static final String COSTO_REVIVIR = "COSTO_REVIVIR";
    public static final String HORAS_RECUPERACION = "HORAS_RECUPERACION";
    public static final String ABONO_RECUPERACION = "ABONO_RECUPERACION";
    public static final String SALUD_REVIVIDA = "SALUD_REVIVIDA";

    private final ParametroJuegoRepository repo;

    public ParametrosJuego(ParametroJuegoRepository repo) {
        this.repo = repo;
    }

    public int valor(String clave, int defecto) {
        return repo.findById(clave).map(ParametroJuego::getValor).orElse(defecto);
    }

    public int costoRevivir() {
        return valor(COSTO_REVIVIR, 250);
    }

    public int horasRecuperacion() {
        return valor(HORAS_RECUPERACION, 24);
    }

    public int abonoRecuperacion() {
        return valor(ABONO_RECUPERACION, 45);
    }

    public int saludRevivida() {
        return valor(SALUD_REVIVIDA, 50);
    }
}
