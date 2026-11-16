package com.plantia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class PlantiaApplication {

    /** Hora con la que se guardan y comparan las fechas de la base (columnas sin zona); Django usa la misma. */
    public static final String ZONA_HORARIA = "America/Lima";

    public static void main(String[] args) {
        // Antes de abrir la conexion: la zona de la sesion de PostgreSQL sale de la de la JVM (CURRENT_TIMESTAMP)
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        SpringApplication.run(PlantiaApplication.class, args);
    }
}
