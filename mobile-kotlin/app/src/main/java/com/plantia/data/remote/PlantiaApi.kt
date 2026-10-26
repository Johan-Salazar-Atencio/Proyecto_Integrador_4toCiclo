package com.plantia.data.remote

import com.plantia.model.PlantSummary
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PlantiaApi {

    /** Sube la foto al backend; este identifica la planta con IA y la guarda. */
    @Multipart
    @POST("api/v1/escaneos")
    suspend fun escanear(
        @Header("X-User-Id") userId: Int,
        @Part foto: MultipartBody.Part
    ): ScanResultDto

    /** Planta en crianza del usuario. HTTP 204 (cuerpo vacio) si no tiene ninguna. */
    @GET("api/v1/plantas/activa")
    suspend fun plantaActiva(
        @Header("X-User-Id") userId: Int
    ): Response<PlantSummary>

    /** Plantas escaneadas del usuario, la mas reciente primero. */
    @GET("api/v1/coleccion")
    suspend fun coleccion(
        @Header("X-User-Id") userId: Int
    ): List<ColeccionItemDto>

    /** Una planta de la coleccion con sus cuidados. */
    @GET("api/v1/coleccion/{id}")
    suspend fun coleccionDetalle(
        @Path("id") id: Int
    ): ColeccionDetalleDto
}
