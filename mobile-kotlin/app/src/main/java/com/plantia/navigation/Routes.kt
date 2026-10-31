package com.plantia.navigation

/** Rutas de la app. Las que reciben argumentos exponen un helper para construirlas. */
object Routes {
    // Entrada
    const val WELCOME = "welcome"
    const val TERMINOS = "terminos"

    const val ARG_MODO = "modo"
    const val AUTH = "auth/{$ARG_MODO}"

    fun auth(registro: Boolean) = "auth/${if (registro) "registro" else "login"}"

    // App
    const val HOME = "home"
    const val SCAN = "ScanScreen"
    const val HISTORIAL = "historial"
    const val VIRTUAL_GARDEN = "VirtualGardenScreen"
    const val COLLECTION = "CollectionScreen"
    const val PERFIL = "perfil"

    const val ARG_PLANT_ID = "plantId"
    const val PLANT_DETAIL = "PlantDetailScreen/{$ARG_PLANT_ID}"

    fun plantDetail(plantId: String) = "PlantDetailScreen/$plantId"

    const val ARG_COLECCION_ID = "coleccionId"
    const val COLLECTION_DETAIL = "CollectionDetailScreen/{$ARG_COLECCION_ID}"

    fun collectionDetail(id: Int) = "CollectionDetailScreen/$id"

    // Catalogo
    const val CATALOGO = "catalogo"
    const val ARG_CATALOGO_ID = "id"
    const val CATALOGO_DETALLE = "catalogo/{$ARG_CATALOGO_ID}"

    fun catalogoDetalle(id: Int) = "catalogo/$id"
}
