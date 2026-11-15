package com.plantia.data.repository

import com.plantia.BuildConfig
import com.plantia.data.model.AnimacionDesastre
import com.plantia.data.model.Arte
import com.plantia.data.model.FondoEscena
import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fondos y animaciones de desastre que dibuja la escena del jardin. */
interface ArteRepository {
    /** El arte, o null si no se pudo leer (la escena entonces usa su fondo de color). */
    suspend fun cargar(): Arte?

    /** Olvida el arte guardado (tras equipar o quitar un fondo de la tienda). */
    fun invalidar() {}
}

class RemoteArteRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api },
    private val baseUrl: () -> String = { (RetrofitClient.baseUrl.value ?: BuildConfig.API_BASE_URL).trimEnd('/') }
) : ArteRepository {

    // El arte casi no cambia: se lee una vez por servidor y se reutiliza (las URLs dependen del host)
    private var cache: Pair<String, Arte>? = null

    override suspend fun cargar(): Arte? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        cache?.takeIf { it.first == base }?.let { return@withContext it.second }
        val servicio = api()
        val fondos = runCatching { servicio.fondos() }.getOrNull()?.takeIf { it.isSuccessful }?.body()
        val animaciones = runCatching { servicio.animaciones() }.getOrNull()?.takeIf { it.isSuccessful }?.body()
        if (fondos == null && animaciones == null) return@withContext null

        val arte = Arte(
            fondos = fondos.orEmpty().filter { it.imageUrl.isNotBlank() }.map { f ->
                FondoEscena(
                    momento = f.momento,
                    imageUrl = absoluta(base, f.imageUrl),
                    frames = f.frames.coerceAtLeast(1),
                    frameWidthPx = f.widthPx,
                    frameHeightPx = f.heightPx,
                    msPerFrame = f.msPerFrame.coerceAtLeast(MS_MINIMO),
                    esPredeterminado = f.isDefault,
                    equipado = f.equipado
                )
            },
            animaciones = animaciones.orEmpty().filter { it.frames.isNotEmpty() }.map { a ->
                AnimacionDesastre(
                    disasterType = a.disasterType,
                    phase = a.phase,
                    msPerFrame = a.msPerFrame.coerceAtLeast(MS_MINIMO),
                    frames = a.frames.map { absoluta(base, it) }
                )
            }
        )
        cache = base to arte
        arte
    }

    override fun invalidar() {
        cache = null
    }

    private fun absoluta(base: String, ruta: String): String =
        if (ruta.startsWith("http")) ruta.replaceFirst(Regex("^https?://[^/]+"), base) else base + "/" + ruta.trimStart('/')

    private companion object {
        /** Nunca menos de 50 ms por cuadro: un valor raro del servidor no debe saturar la pantalla. */
        const val MS_MINIMO = 50
    }
}
