package com.plantia.model

/** Item de "Mi coleccion": una especie vista por el usuario. */
data class ColeccionItem(
    val id: Int,
    val idCatalogo: Int?,
    val nombreComun: String,
    val nombreCientifico: String,
    val fotoUrl: String?,
    val vecesVista: Int,
    val primeraVez: String?,
    val ultimaVez: String?,
    val esToxicaMascotas: Boolean
)

/** Plant del catalogo general. */
data class CatalogoItem(
    val id: Int,
    val nombreComun: String,
    val nombreCientifico: String,
    val frecuenciaRiegoDias: Int,
    val frecuenciaSolHoras: Int,
    val temperaturaMinC: Int?,
    val temperaturaMaxC: Int?,
    val esToxicaMascotas: Boolean,
    val spriteBaseUrl: String
)

/** Seccion de cuidado de una planta. */
data class Cuidado(
    val id: Int,
    val tipo: String,
    val titulo: String,
    val contenido: String,
    val orden: Int,
    val generadoPorIa: Boolean
)
