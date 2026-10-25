package com.plantia.navigation

/** Rutas de la app. Las que reciben argumentos exponen un helper para construirlas. */
object Routes {
    const val HOME = "home"
    const val SCAN = "ScanScreen"
    const val VIRTUAL_GARDEN = "VirtualGardenScreen"
    const val COLLECTION = "CollectionScreen"

    const val ARG_PLANT_ID = "plantId"
    const val PLANT_DETAIL = "PlantDetailScreen/{$ARG_PLANT_ID}"

    fun plantDetail(plantId: String) = "PlantDetailScreen/$plantId"
}
