package com.plantia.data.remote.dto

/** GET /api/v1/tienda: las monedas y el nivel del usuario, y los articulos activos con su estado. */
data class TiendaDto(
    val monedas: Int = 0,
    val nivel: Int = 1,
    val items: List<TiendaItemDto> = emptyList()
)

data class TiendaItemDto(
    val id: Int = 0,
    val nombre: String = "",
    /** ABONO (se usa al comprarlo) o FONDO (se equipa). */
    val tipo: String = "",
    val descripcion: String = "",
    val precio: Int = 0,
    val nivelRequerido: Int = 1,
    val previewUrl: String? = null,
    /** Solo fondos: la escena de cada momento del dia del tema. */
    val variantes: List<VarianteDto> = emptyList(),
    val comprado: Boolean = false,
    val bloqueado: Boolean = false,
    /** Macetas e invernaderos: el identificador con el que la app los dibuja. */
    val codigo: String? = null
)

/** Escena de un momento del dia (DIA, TARDE o NOCHE) con su vista previa. */
data class VarianteDto(
    val momento: String = "",
    val vistaUrl: String = ""
)

/** GET /api/v1/plantas/{id}/fondos: un tema de fondo que se le puede poner a la planta. */
data class TemaFondoDto(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String = "",
    val predeterminado: Boolean = false,
    val enUso: Boolean = false,
    val variantes: List<VarianteDto> = emptyList()
)

/** PUT /api/v1/plantas/macetas: pone una maceta que el usuario tiene a las plantas que elija. */
data class PonerMacetaRequest(val idItem: Int, val ids: List<Int>)

/** PUT /api/v1/plantas/{id}/apodo (vacio = el nombre comun de la especie). */
data class ApodoRequest(val apodo: String)

/** PUT /api/v1/usuarios/me/invernadero */
data class ElegirInvernaderoRequest(val idItem: Int)
