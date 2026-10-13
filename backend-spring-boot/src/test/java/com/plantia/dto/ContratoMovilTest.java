package com.plantia.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Verifica que el JSON que expone la API coincide EXACTAMENTE con los modelos
 * del movil (UserSummary, WeatherAlert, PlantSummary, DailyMission,
 * QuickAccessItem, RecentScan). No requiere base de datos.
 */
class ContratoMovilTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private List<String> claves(Object dto) throws Exception {
        JsonNode nodo = mapper.readTree(mapper.writeValueAsString(dto));
        List<String> claves = new ArrayList<>();
        nodo.fieldNames().forEachRemaining(claves::add);
        return claves;
    }

    @Test
    void userSummary() throws Exception {
        assertThat(claves(new UserSummaryDto("Johan", 5, "Cuidador", 320, 2450, 3000, null)))
                .containsExactlyInAnyOrder(
                        "name", "level", "badge", "leaves", "experience",
                        "experienceToNextLevel", "profileImageUrl");
    }

    @Test
    void weatherAlert() throws Exception {
        assertThat(claves(new WeatherAlertDto("t", "d", "HIGH", "01h 20m", "Refugiar Planta")))
                .containsExactlyInAnyOrder(
                        "title", "description", "severity", "timeRemaining", "actionText");
    }

    @Test
    void plantSummary() throws Exception {
        assertThat(claves(new PlantSummaryDto("Monstera", "Monstera deliciosa", 80, 60, 90, "Hace 2 dias", null)))
                .containsExactlyInAnyOrder(
                        "name", "species", "healthPercent", "humidityPercent",
                        "lightPercent", "lastWatered", "imageUrl");
    }

    @Test
    void dailyMissionIncluyeIsCompleted() throws Exception {
        assertThat(claves(new DailyMissionDto("m1", "Riego", "+50", 0.5f, 1, 2, false)))
                .containsExactlyInAnyOrder(
                        "id", "title", "reward", "progress",
                        "currentCount", "targetCount", "isCompleted");
    }

    @Test
    void quickAccessItem() throws Exception {
        assertThat(claves(new QuickAccessDto("scan", "Escanear", "Identificar", "camera", "#E8F8F0", "#22C55E")))
                .containsExactlyInAnyOrder(
                        "id", "title", "subtitle", "icon", "backgroundColor", "iconColor");
    }

    @Test
    void recentScan() throws Exception {
        assertThat(claves(new RecentScanDto("1", "Monstera", "Confianza 92.5%", "#10B981")))
                .containsExactlyInAnyOrder("id", "plantName", "confidence", "statusColor");
    }

    @Test
    void scanResultYCuidados() throws Exception {
        CuidadoSeccionDto cuidado = new CuidadoSeccionDto(1, "RIEGO", "Riego", "cada 7 dias", 0, true);
        assertThat(claves(cuidado))
                .containsExactlyInAnyOrder("id", "tipo", "titulo", "contenido", "orden", "generadoPorIa");

        ScanResultDto scan = new ScanResultDto(1, 2, "Monstera", "Monstera deliciosa", 92.5,
                "/files/x.jpg", true, List.of(cuidado));
        assertThat(claves(scan))
                .containsExactlyInAnyOrder(
                        "idEscaneo", "idCatalogo", "nombreComun", "nombreCientifico",
                        "confianza", "imagenUrl", "nuevoEnColeccion", "cuidados");
    }
}
