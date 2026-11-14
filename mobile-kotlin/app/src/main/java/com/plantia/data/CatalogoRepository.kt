package com.plantia.data

import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.model.CatalogoItem
import com.plantia.model.Cuidado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Una planta del catalogo con sus secciones de cuidado; `planta` es null si no existe. */
data class FichaCatalogo(val planta: CatalogoItem?, val cuidados: List<Cuidado>)

/** Catalogo general de plantas. Las funciones lanzan excepcion si no hay conexion. */
interface CatalogoRepository {
    suspend fun buscar(consulta: String, soloToxicas: Boolean): List<CatalogoItem>

    suspend fun ficha(idCatalogo: Int): FichaCatalogo
}

class RemoteCatalogoRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : CatalogoRepository {

    override suspend fun buscar(consulta: String, soloToxicas: Boolean): List<CatalogoItem> =
        withContext(Dispatchers.IO) {
            api().catalogo(
                q = consulta.trim().ifBlank { null },
                toxica = if (soloToxicas) true else null
            ).body().orEmpty().map { it.toDomain() }
        }

    override suspend fun ficha(idCatalogo: Int): FichaCatalogo = withContext(Dispatchers.IO) {
        val servicio = api()
        val planta = servicio.catalogo().body().orEmpty().firstOrNull { it.id == idCatalogo }
        val cuidados = servicio.cuidadosCatalogo(idCatalogo).body().orEmpty()
        FichaCatalogo(planta?.toDomain(), cuidados.map { it.toDomain() })
    }
}
