package com.plantia.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Coincide con PlantSummary del movil (los 7 primeros campos). Los demas los usa
 * el jardin de la web; el movil los ignora.
 */
public record PlantSummaryDto(
        String name,
        String species,
        int healthPercent,
        int humidityPercent,
        int lightPercent,
        String lastWatered,
        String imageUrl,
        Integer id,
        int crecimiento,
        Map<String, Object> apariencia,
        String fotoUrl,
        boolean activa,
        /** true mientras la planta esta a resguardo de un desastre. */
        boolean refugiada,
        /** Dias con cuidado contados, dias necesarios para crecer del todo y fase (SEMILLA, BROTE, ADULTA o MUERTA). */
        int diasCuidados,
        int diasTotales,
        String fase,
        /** true mientras la planta, golpeada por un evento, se puede salvar con abono antes de que muera. */
        boolean enRecuperacion,
        /** Segundos que quedan para abonarla; null si no esta en recuperacion. */
        @JsonInclude(JsonInclude.Include.NON_NULL) Long recuperacionSegundos,
        /** Evento que la golpeo y cuya animacion aun no se mostro (ver POST /plantas/{id}/golpe-visto); null si no hay. */
        @JsonInclude(JsonInclude.Include.NON_NULL) String golpeTipo,
        /** Hojas que cuesta revivirla; solo se envia cuando la planta esta muerta. */
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer costoRevivir,
        /** true si esta guardada en el baul: congelada, solo ocupa espacio. */
        boolean enBaul,
        /** Codigo de la maceta que tiene puesta (TERRACOTA si no eligio otra); la app la dibuja con ese codigo. */
        String macetaCodigo,
        /** XP y nivel ganados con la accion (digitalizar, cuidar); no se envia en las consultas. */
        @JsonInclude(JsonInclude.Include.NON_NULL) ProgresoDto progreso
) {
    /** Sin progreso: consultas del jardin. */
    public PlantSummaryDto(String name, String species, int healthPercent, int humidityPercent,
                           int lightPercent, String lastWatered, String imageUrl, Integer id,
                           int crecimiento, Map<String, Object> apariencia, String fotoUrl,
                           boolean activa, boolean refugiada,
                           int diasCuidados, int diasTotales, String fase) {
        this(name, species, healthPercent, humidityPercent, lightPercent, lastWatered, imageUrl,
                id, crecimiento, apariencia, fotoUrl, activa, refugiada, diasCuidados, diasTotales, fase,
                false, null, null, null, false, "TERRACOTA", null);
    }

    /** Solo los campos del contrato con el movil. */
    public PlantSummaryDto(String name, String species, int healthPercent, int humidityPercent,
                           int lightPercent, String lastWatered, String imageUrl) {
        this(name, species, healthPercent, humidityPercent, lightPercent, lastWatered, imageUrl,
                null, 0, null, null, false, false, 0, 0, null, false, null, null, null, false, "TERRACOTA", null);
    }

    public PlantSummaryDto conProgreso(ProgresoDto progreso) {
        return new PlantSummaryDto(name, species, healthPercent, humidityPercent, lightPercent, lastWatered,
                imageUrl, id, crecimiento, apariencia, fotoUrl, activa, refugiada, diasCuidados, diasTotales, fase,
                enRecuperacion, recuperacionSegundos, golpeTipo, costoRevivir, enBaul, macetaCodigo, progreso);
    }
}
