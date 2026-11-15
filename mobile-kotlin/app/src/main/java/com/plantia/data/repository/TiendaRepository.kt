package com.plantia.data.repository

import com.google.gson.JsonParser
import com.plantia.BuildConfig
import com.plantia.data.mapper.toDomain
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.TemaFondo
import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.data.model.VarianteFondo
import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.dto.TemaFondoDto
import com.plantia.data.remote.dto.TiendaDto
import com.plantia.data.remote.dto.VarianteDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed interface TiendaResultado {
    data class Con(val tienda: Tienda) : TiendaResultado
    data class Fallo(val mensaje: String) : TiendaResultado
}

sealed interface AbonoResultado {
    data class Con(val planta: PlantSummary) : AbonoResultado
    data class Fallo(val mensaje: String) : AbonoResultado
}

sealed interface FondosResultado {
    data class Con(val temas: List<TemaFondo>) : FondosResultado
    data class Fallo(val mensaje: String) : FondosResultado
}

/**
 * Tienda: los fondos son temas (una escena de dia, otra de tarde y otra de noche) que se compran una vez y
 * se ponen a cada planta desde el jardin; el abono se compra y se usa en el momento.
 */
interface TiendaRepository {
    suspend fun cargar(): TiendaResultado

    suspend fun comprar(idItem: Int): TiendaResultado

    /** Compra un abono y lo usa en la planta: +1 dia de crecimiento (maximo uno al dia por planta). */
    suspend fun usarAbono(idPlanta: Int): AbonoResultado

    /** Temas que se le pueden poner a esa planta (el de siempre y los comprados), con el que tiene puesto. */
    suspend fun fondosDePlanta(idPlanta: Int): FondosResultado

    /** Pone un tema a la planta; `null` la devuelve al fondo de siempre. */
    suspend fun ponerFondo(idPlanta: Int, idTema: Int?): FondosResultado
}

class RemoteTiendaRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api },
    private val baseUrl: () -> String = { (RetrofitClient.baseUrl.value ?: BuildConfig.API_BASE_URL).trimEnd('/') },
    /** Poner otro fondo cambia la escena: el arte guardado debe leerse otra vez. */
    private val arte: () -> ArteRepository? = { null }
) : TiendaRepository {

    override suspend fun cargar(): TiendaResultado = tienda { api().tienda() }

    override suspend fun comprar(idItem: Int): TiendaResultado = tienda { api().comprarItem(idItem) }

    override suspend fun usarAbono(idPlanta: Int): AbonoResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().abonoTienda(idPlanta)
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) AbonoResultado.Con(cuerpo.toDomain())
            else AbonoResultado.Fallo(mensajeDe(respuesta))
        } catch (e: Exception) {
            AbonoResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    override suspend fun fondosDePlanta(idPlanta: Int): FondosResultado = fondos { api().fondosPlanta(idPlanta) }

    override suspend fun ponerFondo(idPlanta: Int, idTema: Int?): FondosResultado =
        fondos { api().ponerFondo(idPlanta, if (idTema != null) mapOf("idTema" to idTema) else emptyMap()) }
            .also { if (it is FondosResultado.Con) arte()?.invalidar() }

    private suspend fun tienda(llamada: suspend () -> Response<TiendaDto>): TiendaResultado =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = llamada()
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) TiendaResultado.Con(cuerpo.aTienda())
                else TiendaResultado.Fallo(mensajeDe(respuesta))
            } catch (e: Exception) {
                TiendaResultado.Fallo(e.message ?: SIN_CONEXION)
            }
        }

    private suspend fun fondos(llamada: suspend () -> Response<List<TemaFondoDto>>): FondosResultado =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = llamada()
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) FondosResultado.Con(cuerpo.map { it.aTema() })
                else FondosResultado.Fallo(mensajeDe(respuesta))
            } catch (e: Exception) {
                FondosResultado.Fallo(e.message ?: SIN_CONEXION)
            }
        }

    /** Spring responde {"error": "..."}; se muestra ese texto (monedas, nivel, ya lo tienes...). */
    private fun mensajeDe(respuesta: Response<*>): String {
        val texto = runCatching { respuesta.errorBody()?.string() }.getOrNull()
        val error = runCatching { JsonParser.parseString(texto).asJsonObject.get("error").asString }.getOrNull()
        return error ?: "El servidor respondio ${respuesta.code()}"
    }

    private fun TiendaDto.aTienda(): Tienda {
        val base = baseUrl()
        return Tienda(
            monedas = monedas,
            nivel = nivel,
            items = items.map {
                TiendaItem(
                    id = it.id, nombre = it.nombre, tipo = it.tipo, descripcion = it.descripcion, precio = it.precio,
                    nivelRequerido = it.nivelRequerido,
                    previewUrl = it.previewUrl?.takeIf { url -> url.isNotBlank() }?.let { url -> absoluta(base, url) },
                    variantes = it.variantes.aVariantes(base),
                    comprado = it.comprado, bloqueado = it.bloqueado
                )
            }
        )
    }

    private fun TemaFondoDto.aTema(): TemaFondo =
        TemaFondo(id, nombre, descripcion, predeterminado, enUso, variantes.aVariantes(baseUrl()))

    private fun List<VarianteDto>.aVariantes(base: String): List<VarianteFondo> =
        filter { it.vistaUrl.isNotBlank() }.map { VarianteFondo(it.momento, absoluta(base, it.vistaUrl)) }

    private fun absoluta(base: String, ruta: String): String =
        if (ruta.startsWith("http")) ruta.replaceFirst(Regex("^https?://[^/]+"), base) else base + "/" + ruta.trimStart('/')

    private companion object {
        const val SIN_CONEXION = "No se pudo conectar con el servidor"
    }
}
