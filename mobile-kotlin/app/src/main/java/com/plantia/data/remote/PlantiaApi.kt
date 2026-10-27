package com.plantia.data.remote

import com.plantia.model.PlantSummary
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantiaApi {

    // ─── Scan ────────────────────────────────────────────────────────────────

    /** Sube la foto al backend; este identifica la planta con IA y la guarda. */
    @Multipart
    @POST("api/v1/escaneos")
    suspend fun escanear(
        @Header("X-User-Id") userId: Int,
        @Part foto: MultipartBody.Part
    ): ScanResultDto

    // ─── Home (feat/movil) ──────────────────────────────────────────────────

    /** Accesos rapidos del menu. */
    @GET("api/v1/accesos")
    suspend fun accesos(): Response<List<QuickAccessDto>>

    /** Perfil del usuario (404 si no existe). */
    @GET("api/v1/usuarios/me")
    suspend fun usuarioMe(): Response<UserSummaryDto>

    /** Alerta climatica activa (204 si no hay ninguna). */
    @GET("api/v1/eventos/activo")
    suspend fun eventoActivo(): Response<WeatherAlertDto>

    /** Planta en crianza del usuario (204 si no tiene). */
    @GET("api/v1/plantas/activa")
    suspend fun plantaActiva(): Response<PlantSummaryDto>

    /** Mision del dia (204 si no hay misiones activas). */
    @GET("api/v1/misiones/diaria")
    suspend fun misionDiaria(): Response<DailyMissionDto>

    /** Historial de escaneos. */
    @GET("api/v1/escaneos/recientes")
    suspend fun escaneosRecientes(): Response<List<RecentScanDto>>

    /** Catalogo de plantas, con busqueda y filtro de toxicidad. */
    @GET("api/v1/catalogo")
    suspend fun catalogo(
        @Query("q") q: String? = null,
        @Query("toxica") toxica: Boolean? = null
    ): Response<List<CatalogoItemDto>>

    /** Secciones de cuidado de una planta del catalogo. */
    @GET("api/v1/catalogo/{id}/cuidados")
    suspend fun cuidadosCatalogo(
        @Path("id") idCatalogo: Int
    ): Response<List<CuidadoSeccionDto>>

    /** Mi coleccion de plantas vistas. */
    @GET("api/v1/coleccion")
    suspend fun coleccion(): Response<List<ColeccionItemDto>>

    /** Detalle de un item de la coleccion, con sus cuidados. */
    @GET("api/v1/coleccion/{id}")
    suspend fun coleccionDetalle(
        @Path("id") idColeccion: Int
    ): Response<ColeccionDetalleDto>

    // ─── Auth (HEAD/main) ───────────────────────────────────────────────────

    /** Planta en crianza del usuario con userId explicito (auth header). */
    @GET("api/v1/plantas/activa")
    suspend fun plantaActivaAuth(
        @Header("X-User-Id") userId: Int
    ): Response<PlantSummary>

    /** Plantas escaneadas del usuario, la mas reciente primero (con userId). */
    @GET("api/v1/coleccion")
    suspend fun coleccionAuth(
        @Header("X-User-Id") userId: Int
    ): List<ColeccionItemDto>

    /** Una planta de la coleccion con sus cuidados (con userId). */
    @GET("api/v1/coleccion/{id}")
    suspend fun coleccionDetalleAuth(
        @Header("X-User-Id") userId: Int,
        @Path("id") id: Int
    ): ColeccionDetalleDto

    /** Registrar nuevo usuario. */
    @POST("api/v1/auth/registro")
    suspend fun registrar(@Body request: AuthRegistroRequestDto): AuthUsuarioResponseDto

    /** Login de usuario. */
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: AuthLoginRequestDto): AuthUsuarioResponseDto
}

// ─── DTOs Home (feat/movil) ─────────────────────────────────────────────────

/** GET /api/v1/escaneos/recientes */
data class RecentScanDto(
    val id: String = "",
    val plantName: String = "",
    val confidence: String = "",
    val statusColor: String = "#10B981"
)

/** GET /api/v1/catalogo */
data class CatalogoItemDto(
    val id: Int = 0,
    val nombreComun: String = "",
    val nombreCientifico: String = "",
    val frecuenciaRiegoDias: Int = 0,
    val frecuenciaSolHoras: Int = 0,
    val temperaturaMinC: Int? = null,
    val temperaturaMaxC: Int? = null,
    val esToxicaMascotas: Boolean = false,
    val spriteBaseUrl: String = ""
)

/** GET /api/v1/coleccion */
data class ColeccionItemDto(
    val id: Int = 0,
    val idCatalogo: Int? = null,
    val nombreComun: String = "",
    val nombreCientifico: String = "",
    val fotoUrl: String? = null,
    val vecesVista: Int = 0,
    val primeraVez: String? = null,
    val ultimaVez: String? = null,
    val esToxicaMascotas: Boolean = false
)

/** GET /api/v1/coleccion/{id} */
data class ColeccionDetalleDto(
    val item: ColeccionItemDto = ColeccionItemDto(),
    val cuidados: List<CuidadoSeccionDto> = emptyList()
)

/** DTO for auth */
data class AuthRegistroRequestDto(
    val nombre: String,
    val email: String,
    val password: String
)

data class AuthLoginRequestDto(
    val email: String,
    val password: String
)

data class AuthUsuarioResponseDto(
    val id: Int = 0,
    val nombre: String = "",
    val email: String = ""
)

/** DTO for user summary */
data class UserSummaryDto(
    val nombre: String = "",
    val nivel: Int = 1,
    val insignia: String = "",
    val hojas: Int = 0,
    val experiencia: Int = 0,
    val experienciaSiguienteNivel: Int = 0
)

/** DTO for weather alert */
data class WeatherAlertDto(
    val titulo: String = "",
    val descripcion: String = "",
    val severidad: String = "LOW",
    val tiempoRestante: String = "",
    val textoAccion: String = ""
)

/** DTO for quick access items */
data class QuickAccessDto(
    val texto: String = "",
    val tipo: String = "",
    val icono: String = ""
)

/** DTO for daily mission */
data class DailyMissionDto(
    val titulo: String = "",
    val descripcion: String = "",
    val recompensa: Int = 0,
    val completada: Boolean = false
)

/** DTO for plant summary */
data class PlantSummaryDto(
    val nombre: String = "",
    val especie: String = "",
    val diasPlantada: Int = 0,
    val proximoRiego: String = "",
    val estadoSalud: String = "BUENA",
    val fotoUrl: String? = null
)

/** DTO for care sections */
data class CuidadoSeccionDto(
    val titulo: String = "",
    val items: List<String> = emptyList()
)
