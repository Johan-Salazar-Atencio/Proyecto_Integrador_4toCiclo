package com.plantia.dto;

import java.time.LocalDateTime;

/** Item del listado de "Mi coleccion". */
public record ColeccionItemDto(
        Integer id,
        Integer idCatalogo,
        String nombreComun,
        String nombreCientifico,
        String fotoUrl,
        int vecesVista,
        LocalDateTime primeraVez,
        LocalDateTime ultimaVez,
        boolean esToxicaMascotas
) {
}
