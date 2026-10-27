package com.plantia.data

import com.plantia.data.remote.CuidadoSeccionDto
import com.plantia.data.remote.ScanResultDto

/** Datos simulados para Previews y desarrollo sin backend. */
object MockPlantData {

    val plantDetail = PlantDetail(
        id = "1",
        commonName = "Monstera",
        species = "Monstera deliciosa",
        confidence = 0.94f,
        diagnosis = "Hojas sanas con leve exceso de sol directo en los bordes.",
        isHealthy = true,
        careLevels = listOf(
            CareLevel("Riego", 0.6f, "Cada 7 dias, deja secar la capa superior."),
            CareLevel("Luz", 0.8f, "Luz brillante indirecta."),
            CareLevel("Humedad", 0.5f, "Rocia las hojas en epoca seca.")
        )
    )

    val plantDetailSick = plantDetail.copy(
        id = "2",
        commonName = "Ficus",
        species = "Ficus elastica",
        confidence = 0.81f,
        diagnosis = "Hojas amarillas: posible exceso de riego.",
        isHealthy = false
    )

    val petStatus = PetStatus(
        plantId = "1",
        nickname = "Monstera",
        healthPercent = 80,
        waterPercent = 60,
        lightPercent = 90
    )

    val collection = listOf(
        CollectionItem("1", "Monstera", level = 5, isLocked = false),
        CollectionItem("2", "Ficus", level = 3, isLocked = false),
        CollectionItem("3", "Cactus", level = 2, isLocked = false),
        CollectionItem("4", "???", level = 0, isLocked = true),
        CollectionItem("5", "Helecho", level = 1, isLocked = false),
        CollectionItem("6", "???", level = 0, isLocked = true)
    )

    fun detailById(id: String?): PlantDetail =
        listOf(plantDetail, plantDetailSick).firstOrNull { it.id == id } ?: plantDetail

    /** Respuesta simulada con la misma forma que POST /api/v1/escaneos. */
    val scanResult = ScanResultDto(
        idEscaneo = 12,
        idCatalogo = 1,
        nombreComun = "Monstera",
        nombreCientifico = "Monstera deliciosa",
        confianza = 94.0,
        imagenUrl = null,
        nuevoEnColeccion = true,
        cuidados = listOf(
            CuidadoSeccionDto(1, "RIEGO", "Riego moderado", "Riega cuando los primeros 3 cm de sustrato esten secos; evita encharcamientos.", 0, true),
            CuidadoSeccionDto(2, "LUZ", "Luz indirecta brillante", "Prefiere luz indirecta abundante; el sol directo quema sus hojas.", 1, true),
            CuidadoSeccionDto(3, "TEMPERATURA", "Clima calido", "Mantenla entre 15 y 30 C, lejos de corrientes frias.", 2, true),
            CuidadoSeccionDto(4, "SUSTRATO", "Sustrato aireado", "Mezcla con perlita para buen drenaje.", 3, true),
            CuidadoSeccionDto(5, "TOXICIDAD", "Toxica para mascotas", "Contiene oxalato de calcio; puede irritar a perros y gatos.", 4, true)
        )
    )

    val scanResultFallo = ScanResultDto(
        idEscaneo = 13,
        nombreComun = "Planta desconocida",
        confianza = 0.0,
        cuidados = emptyList()
    )
}
