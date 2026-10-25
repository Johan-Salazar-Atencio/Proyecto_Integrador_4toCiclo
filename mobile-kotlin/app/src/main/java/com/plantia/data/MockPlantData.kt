package com.plantia.data

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
}
