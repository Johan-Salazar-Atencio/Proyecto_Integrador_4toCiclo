package com.plantia.data.remote

import com.plantia.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val RUTA_SALUD = "/api/v1/usuarios/me"

    /**
     * Candidatos en orden de preferencia: la URL principal y los respaldos.
     * Permite trabajar por WiFi (IP de la PC) y, si esa red falla, seguir por
     * USB con `adb reverse` (127.0.0.1) o con el emulador (10.0.2.2).
     */
    private val candidatos: List<String> =
        (listOf(BuildConfig.API_BASE_URL) + BuildConfig.API_FALLBACK_URLS.split(","))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private val cliente: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // La IA (nube o local) puede tardar en responder a la primera inferencia.
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    /** URL base efectiva: la primera que responde; si ninguna, la principal. */
    val baseUrlEfectiva: String by lazy { resolverBaseUrl() }

    val api: PlantiaApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrlEfectiva)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlantiaApi::class.java)
    }

    private fun resolverBaseUrl(): String {
        for (url in candidatos) {
            if (responde(url)) {
                println("[RetrofitClient] Backend disponible en $url")
                return normalizar(url)
            }
        }
        val principal = candidatos.firstOrNull() ?: BuildConfig.API_BASE_URL
        println("[RetrofitClient] Ningun backend respondio; se usara $principal")
        return normalizar(principal)
    }

    private fun responde(baseUrl: String): Boolean = try {
        val sonda = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(normalizar(baseUrl).trimEnd('/') + RUTA_SALUD)
            .get()
            .build()
        sonda.newCall(request).execute().use { it.isSuccessful }
    } catch (e: Exception) {
        false
    }

    /** Retrofit exige que la base URL termine en "/". */
    private fun normalizar(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    /**
     * Convierte una URL del backend en una que funcione desde el dispositivo.
     *
     * El backend devuelve URLs como `http://localhost:8080/files/foto.jpg`, pero
     * en el telefono `localhost` es el propio telefono. Se reemplaza el host por
     * el del backend que se esta usando.
     */
    fun urlAbsoluta(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!url.startsWith("http")) return null
        val base = baseUrlEfectiva.trimEnd('/')
        return url.replaceFirst(Regex("^https?://[^/]+"), base)
    }
}
