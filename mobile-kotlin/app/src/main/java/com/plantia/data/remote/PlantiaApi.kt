package com.plantia.data.remote

import com.plantia.data.remote.dto.AuthLoginRequestDto
import com.plantia.data.remote.dto.AuthRegistroRequestDto
import com.plantia.data.remote.dto.AuthUsuarioResponseDto
import com.plantia.data.remote.dto.CatalogoItemDto
import com.plantia.data.remote.dto.ColeccionDetalleDto
import com.plantia.data.remote.dto.ColeccionItemDto
import com.plantia.data.remote.dto.CuidadoSeccionDto
import com.plantia.data.remote.dto.DailyMissionDto
import com.plantia.data.remote.dto.PlantSummaryDto
import com.plantia.data.remote.dto.QuickAccessDto
import com.plantia.data.remote.dto.RecentScanDto
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.data.remote.dto.UserSummaryDto
import com.plantia.data.remote.dto.WeatherAlertDto
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

/**
 * Endpoints del backend (Spring Boot).
 *
 * El id del usuario viaja en el header `X-User-Id`, que `RetrofitClient` agrega a todas las
 * peticiones a partir de `Sesion`; por eso la mayoria de funciones no lo piden.
 * Los endpoints que pueden responder 204 devuelven `Response<...>`.
 */
interface PlantiaApi {

    // Escaneo

    /** Sube la foto al backend; este identifica la planta con IA y la guarda. */
    @Multipart
    @POST("api/v1/escaneos")
    suspend fun escanear(
        @Header("X-User-Id") userId: Int,
        @Part foto: MultipartBody.Part
    ): ScanResultDto

    /** Historial de escaneos. */
    @GET("api/v1/escaneos/recientes")
    suspend fun escaneosRecientes(): Response<List<RecentScanDto>>

    // Home

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

    /** Todas las plantas digitales del usuario. */
    @GET("api/v1/plantas")
    suspend fun plantas(): Response<List<PlantSummaryDto>>

    /** Digitaliza una planta y la deja activa. Body: { "idEscaneo": n } o { "idCatalogo": n } */
    @POST("api/v1/plantas")
    suspend fun digitalizar(@Body body: Map<String, Int>): Response<PlantSummaryDto>

    /** Elige la planta que se muestra en el jardin. */
    @POST("api/v1/plantas/{id}/seleccionar")
    suspend fun seleccionarPlanta(@Path("id") idPlanta: Int): Response<PlantSummaryDto>

    /** Aplica un cuidado. Body: { "accion": "REGAR" | "SOL" | "ABONAR" } */
    @POST("api/v1/plantas/{id}/cuidados")
    suspend fun cuidarPlanta(
        @Path("id") idPlanta: Int,
        @Body body: Map<String, String>
    ): Response<PlantSummaryDto>

    // Catalogo

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

    // Coleccion

    /** Mi coleccion de plantas escaneadas. */
    @GET("api/v1/coleccion")
    suspend fun coleccion(): Response<List<ColeccionItemDto>>

    /** Agrega una planta a la coleccion del usuario (idempotente). Body: { "idEscaneo": <int> } */
    @POST("api/v1/coleccion")
    suspend fun agregarColeccion(
        @Body body: Map<String, Int>
    ): Response<ColeccionItemDto>

    /** Detalle de un item de la coleccion, con sus cuidados. */
    @GET("api/v1/coleccion/{id}")
    suspend fun coleccionDetalle(
        @Path("id") idColeccion: Int
    ): Response<ColeccionDetalleDto>

    // Acceso

    @POST("api/v1/auth/registro")
    suspend fun registrar(@Body request: AuthRegistroRequestDto): AuthUsuarioResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: AuthLoginRequestDto): AuthUsuarioResponseDto
}
