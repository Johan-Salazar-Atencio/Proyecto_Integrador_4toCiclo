package com.plantia.fakes

import com.plantia.data.model.Logro
import com.plantia.data.model.LogroNuevo
import com.plantia.data.model.LogrosResumen
import com.plantia.data.model.Rareza
import com.plantia.data.repository.LogrosRepository
import com.plantia.data.repository.LogrosResultado

fun logroNuevo(id: Int, rareza: Rareza = Rareza.COMUN, titulo: String = "Logro $id") = LogroNuevo(
    idLogro = id, codigo = "L$id", titulo = titulo, descripcion = "Descripcion $id",
    icono = "hoja", rareza = rareza, recompensaXp = 10, recompensaMonedas = 5
)

fun logro(
    id: Int,
    desbloqueado: Boolean = false,
    progreso: Int = 0,
    meta: Int = 10,
    secreto: Boolean = false
) = Logro(
    idLogro = id, codigo = "L$id", titulo = "Logro $id", descripcion = "Descripcion $id", icono = "hoja",
    rareza = Rareza.COMUN, meta = meta, progreso = progreso, desbloqueado = desbloqueado, secreto = secreto
)

/** Logros falsos: lo que devuelve cada llamada se configura y se anotan los "vistos" que llegaron. */
class FakeLogrosRepository : LogrosRepository {
    var listaResultado: LogrosResultado = LogrosResultado.Con(LogrosResumen(0, 0, emptyList()))
    var pendientesResultado: List<LogroNuevo>? = emptyList()
    var vistosFunciona = true

    /** Cada POST de vistos que se intento (con exito o no). */
    val intentosVistos = mutableListOf<List<Int>>()
    val vistosGuardados = mutableListOf<Int>()
    var consultasPendientes = 0

    override suspend fun listar(): LogrosResultado = listaResultado

    override suspend fun pendientes(): List<LogroNuevo>? {
        consultasPendientes++
        return pendientesResultado
    }

    override suspend fun marcarVistos(ids: List<Int>): Boolean {
        intentosVistos += ids
        if (!vistosFunciona) return false
        vistosGuardados += ids
        return true
    }
}
