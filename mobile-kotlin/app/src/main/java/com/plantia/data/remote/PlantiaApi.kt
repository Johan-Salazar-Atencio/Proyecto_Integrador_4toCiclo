package com.plantia.data.remote

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
    ): Response<PlantSummaryDto>

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
