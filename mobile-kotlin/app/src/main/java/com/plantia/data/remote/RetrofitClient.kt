package com.plantia.data.remote

import androidx.compose.runtime.mutableStateOf
import android.os.Looper
import com.plantia.BuildConfig
import com.plantia.data.ConexionConfig
import com.plantia.data.Sesion
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val RUTA_SALUD = "/api/v1/usuarios/me"

    /** URL base que se esta usando ahora mismo. */
    private val _baseUrl = MutableStateFlow<String?>(null)
    val baseUrl: StateFlow<String?> = _baseUrl

    /** Host descubierto por mDNS en la red local. */
    private val _hostMdns = MutableStateFlow<String?>(null)

    private var instancia: PlantiaApi? = null

    /**
     * Candidatos configurados en la compilacion
     * (`local.properties` o `-PapiBaseUrl=`).
     */
    private val candidatosCompilados: List<String> =
        (listOf(BuildConfig.API_BASE_URL) + BuildConfig.API_FALLBACK_URLS.split(","))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private val cliente: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // La IA (nube o local) puede tardar en responder a la primera inferencia.
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // El id de usuario viaja en todas las peticiones, tomado de la sesion.
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("X-User-Id", Sesion.userId.value.toString())
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    /**
     * Candidatos en orden de preferencia:
     * 1. Host escrito a mano en la app (Ajustes).
     * 2. Host descubierto por mDNS: el PC de la red local, sin conocer su IP.
     * 3. Los configurados al compilar (WiFi con IP fija, USB, emulador).
     */
    private fun candidatos(): List<String> = buildList {
        ConexionConfig.host.value?.let { add(it) }
        _hostMdns.value?.let { add(it) }
        addAll(candidatosCompilados)
    }.distinct()

    val api: PlantiaApi
        get() {
            val actual = instancia
            if (actual != null) return actual
            val url = resolverBaseUrl()
            return crear(url).also {
                instancia = it
                _baseUrl.value = url
            }
        }

    /** Se llama cuando mDNS encuentra el backend en la red local. */
    fun actualizaHostMdns(url: String?) {
        if (_hostMdns.value == url) return
        _hostMdns.value = url
        if (url != null) reconectar()
    }

    /** Fuerza a volver a elegir el mejor host disponible. */
    fun reconectar() {
        instancia = null
        _baseUrl.value = null
    }

    private fun crear(url: String): PlantiaApi = Retrofit.Builder()
        .baseUrl(url)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlantiaApi::class.java)

    private fun resolverBaseUrl(): String {
        for (url in candidatos()) {
            if (responde(url)) {
                println("[RetrofitClient] Backend disponible en $url")
                return normalizar(url)
            }
        }
        val principal = candidatos().firstOrNull() ?: BuildConfig.API_BASE_URL
        println("[RetrofitClient] Ningun backend respondio; se usara $principal")
        return normalizar(principal)
    }

    /**
     * Comprueba si el backend responde en esa URL.
     *
     * La comprobacion es sincrona, pero Android prohibe las llamadas de red en
     * el hilo principal (NetworkOnMainThreadException), asi que si se llama
     * desde alla se hace el sondeo en un hilo de E/S.
     */
    private fun responde(baseUrl: String): Boolean {
        val enHiloPrincipal = Looper.myLooper() == Looper.getMainLooper()
        return if (enHiloPrincipal) {
            runBlocking(Dispatchers.IO) { sona(baseUrl) }
        } else {
            sona(baseUrl)
        }
    }

    private fun sona(baseUrl: String): Boolean = try {
        val sonda = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(normalizar(baseUrl).trimEnd('/') + RUTA_SALUD)
            .get()
            .build()
        val codigo = sonda.newCall(request).execute().use { it.code }
        if (codigo !in 200..299) {
            println("[RetrofitClient] $baseUrl respondio HTTP $codigo")
        }
        codigo in 200..299
    } catch (e: Exception) {
        println("[RetrofitClient] $baseUrl fallo: ${e.javaClass.simpleName}: ${e.message}")
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
        val base = (_baseUrl.value ?: BuildConfig.API_BASE_URL).trimEnd('/')
        return url.replaceFirst(Regex("^https?://[^/]+"), base)
    }
}
