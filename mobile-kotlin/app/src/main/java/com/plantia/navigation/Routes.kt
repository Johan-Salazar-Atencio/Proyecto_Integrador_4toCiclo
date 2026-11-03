package com.plantia.navigation

/** Rutas de la app. Las que reciben argumentos exponen un helper para construirlas. */
object Routes {
    const val WELCOME = "welcome"
    const val TERMINOS = "terminos"

    const val ARG_MODO = "modo"
    const val AUTH = "auth/{$ARG_MODO}"

    fun auth(registro: Boolean) = "auth/${if (registro) "registro" else "login"}"

    const val HOME = "home"
    const val SCAN = "scan"
    const val HISTORIAL = "historial"
    const val COLECCION = "coleccion"
    const val COLECCION_DETALLE = "coleccion/{id}"
    const val ARG_COLECCION_ID = "id"
    const val CATALOGO = "catalogo"
    const val CATALOGO_DETALLE = "catalogo/{id}"
    const val JARDIN = "jardin"
    const val PERFIL = "perfil"

    const val COLLECTION = "collection"
    const val VIRTUAL_GARDEN = "virtualGarden"

    const val ARG_PLANT_ID = "plantId"
    const val PLANT_DETAIL = "plantDetail/{$ARG_PLANT_ID}"

    fun plantDetail(plantId: String) = "plantDetail/$plantId"

    const val COLLECTION_DETAIL = "collection/{id}"

    fun collectionDetail(id: Int) = "collection/$id"
}
