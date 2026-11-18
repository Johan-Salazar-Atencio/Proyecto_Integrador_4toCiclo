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
        assertThat(claves(new UserSummaryDto("Johan", 5, "Cuidador", 320, 2450, 3000, null, "johan@plantia.com", 3, 2450, "LIMA", "SELVA")))
                .containsExactlyInAnyOrder(
                        "name", "level", "badge", "leaves", "experience",
                        "experienceToNextLevel", "profileImageUrl", "email", "streakDays", "experienceFromLevel", "regionCode", "greenhouseCode");
    }

    @Test
    void weatherAlert() throws Exception {
        assertThat(claves(new WeatherAlertDto("t", "d", "HIGH", "01h 20m", "Prevenir evento", "HUAYCO", "PICO")))
                .containsExactlyInAnyOrder(
                        "title", "description", "severity", "timeRemaining", "actionText", "disasterType", "phase");
    }

    @Test
    void laAlertaTraeLosDatosDeLaTarjetaDelEventoSoloSiExisten() throws Exception {
        assertThat(claves(new WeatherAlertDto("t", "d", "HIGH", "01h 20m", "Prevenir evento", "OLA_DE_CALOR", "PICO",
                "Ola de calor", "/files/arte/eventos/ola.jpg", "texto", "consejo", 3600L)))
                .contains("eventName", "imageUrl", "info", "tip", "endsInSeconds");
        assertThat(claves(new WeatherAlertDto("t", "d", "HIGH", "01h 20m", "Prevenir evento", "HUAYCO", "PICO")))
                .doesNotContain("eventName", "imageUrl", "info", "tip", "endsInSeconds");
    }

    @Test
    void plantSummary() throws Exception {
        assertThat(claves(new PlantSummaryDto("Monstera", "Monstera deliciosa", 80, 60, 90, "Hace 2 dias", null)))
                .containsExactlyInAnyOrder(
                        "name", "species", "healthPercent", "humidityPercent",
                        "lightPercent", "lastWatered", "imageUrl",
                        // Extras del jardin web (el movil los ignora)
                        "id", "crecimiento", "apariencia", "fotoUrl", "activa", "refugiada", "diasCuidados", "diasTotales", "fase",
                        "enRecuperacion", "enBaul", "macetaCodigo");
    }

    @Test
    void laMisionDeEventoTraeSusCamposExtra() throws Exception {
        assertThat(claves(new DailyMissionDto("event_1", "Prevenir ola de calor", "+80 XP", 0.5f, 1, 2, false,
                "EVENT", "OLA_DE_CALOR", 7200L, null)))
                .containsExactlyInAnyOrder(
                        "id", "title", "reward", "progress", "currentCount", "targetCount", "isCompleted",
                        "kind", "eventType", "endsInSeconds");
    }

    @Test
    void laMisionDiariaTraeSuTipoParaLlevarAlUsuarioADondeSeCumple() throws Exception {
        assertThat(claves(new DailyMissionDto("m1", "Riego", "+5", 0f, 0, 2, false, null, null, null, "RIEGO")))
                .contains("type")
                .doesNotContain("kind", "eventType", "endsInSeconds");
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
        assertThat(claves(new RecentScanDto("1", "Monstera", "Confianza 92%", 92, "#10B981", "/files/a.jpg")))
                .containsExactlyInAnyOrder("id", "plantName", "confidence", "confianza", "statusColor", "fotoUrl");
    }

    @Test
    void scanResultYCuidados() throws Exception {
        CuidadoSeccionDto cuidado = new CuidadoSeccionDto(1, "RIEGO", "Riego", "cada 7 dias", 0, true);
        assertThat(claves(cuidado))
                .containsExactlyInAnyOrder("id", "tipo", "titulo", "contenido", "orden", "generadoPorIa");

        ScanResultDto scan = new ScanResultDto(1, 2, "Monstera", "Monstera deliciosa", 92.5,
                "/files/x.jpg", true, List.of(cuidado), true);
        assertThat(claves(scan))
                .containsExactlyInAnyOrder(
                        "idEscaneo", "idCatalogo", "nombreComun", "nombreCientifico",
                        "confianza", "imagenUrl", "nuevoEnColeccion", "cuidados", "identificado",
                        "apariencia");
    }

    @Test
    void progresoSoloSeEnviaCuandoHayProgreso() throws Exception {
        ProgresoDto progreso = new ProgresoDto(20, 5, 2, true, 520, 3);
        PlantSummaryDto planta = new PlantSummaryDto("Monstera", "Monstera deliciosa", 80, 60, 90, "Hace 2 dias", null);

        assertThat(claves(planta)).doesNotContain("progreso");
        assertThat(claves(planta.conProgreso(progreso))).contains("progreso");
        assertThat(claves(progreso))
                .containsExactlyInAnyOrder("xpGanado", "monedasGanadas", "nivel", "subioDeNivel", "experienciaXp", "rachaDias");

        ScanResultDto sinProgreso = new ScanResultDto(1, 2, "Monstera", "Monstera deliciosa", 92.5,
                "/files/x.jpg", false, List.of(), true);
        ScanResultDto conProgreso = new ScanResultDto(1, 2, "Monstera", "Monstera deliciosa", 92.5,
                "/files/x.jpg", false, List.of(), true, null, progreso);
        assertThat(claves(sinProgreso)).doesNotContain("progreso");
        assertThat(claves(conProgreso)).contains("progreso");
    }
}
