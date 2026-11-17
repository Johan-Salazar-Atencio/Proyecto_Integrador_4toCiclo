package com.plantia.ui.collection.virtual

import com.plantia.data.model.PlantSummary

/** Plantas que caben en una pantalla del invernadero: 3 por estante, 2 estantes. */
const val PLANTAS_POR_PAGINA = 6

/** Nombre del invernadero cuando la tienda todavia no lo trajo (se muestra el que manda el servidor si existe). */
fun nombreDelInvernadero(codigo: String): String = when (codigo) {
    "VICTORIANO" -> "Invernadero victoriano"
    "LUCES" -> "Luces de noche"
    else -> "Selva tropical"
}

/** Busca por apodo o por especie, sin importar mayusculas; una consulta vacia deja todo. */
fun filtrarPlantas(plantas: List<PlantSummary>, consulta: String): List<PlantSummary> {
    val buscada = consulta.trim().lowercase()
    if (buscada.isEmpty()) return plantas
    return plantas.filter { buscada in it.name.lowercase() || buscada in it.species.lowercase() }
}

/** Las plantas vivas del invernadero repartidas de a 6 por pantalla; siempre hay al menos una pantalla. */
fun paginasDelInvernadero(activas: List<PlantSummary>): List<List<PlantSummary>> =
    activas.chunked(PLANTAS_POR_PAGINA).ifEmpty { listOf(emptyList()) }

/** Cuantas pantallas tiene el carrusel: las del invernadero y, al final, la del baul. */
fun totalDePantallas(activas: List<PlantSummary>): Int = paginasDelInvernadero(activas).size + 1
