package com.plantia.data.remote

import com.plantia.data.remote.dto.PrevenirPlantasRequest
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit valida cada metodo de la API al llamarlo, no al crearla: un tipo no permitido (por ejemplo un
 * Map con una lista) solo falla en el telefono. Aqui se llama a los metodos nuevos con un servidor falso.
 */
class PlantiaApiTest {

    private var ruta = ""
    private var cuerpo = ""

    private fun api(respuesta: String): PlantiaApi {
        val cliente = OkHttpClient.Builder().addInterceptor { cadena ->
            val peticion = cadena.request()
            ruta = "${peticion.method} ${peticion.url.encodedPath}"
            cuerpo = peticion.body?.let { Buffer().also { b -> it.writeTo(b) }.readUtf8() }.orEmpty()
            Response.Builder()
                .request(peticion).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(respuesta.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlantiaApi::class.java)
    }

    @Test
    fun prevenir_varias_plantas_manda_la_lista_de_ids() = runTest {
        val respuesta = api("""[{"name":"Monstera","id":1,"refugiada":true}]""").prevenirPlantas(PrevenirPlantasRequest(listOf(1, 2)))

        assertEquals("POST /api/v1/plantas/prevenir", ruta)
        assertEquals("""{"ids":[1,2]}""", cuerpo)
        assertTrue(respuesta.body()?.first()?.refugiada == true)
    }

    @Test
    fun prevenir_revivir_y_golpe_visto_usan_su_ruta() = runTest {
        val api = api("""{"name":"Monstera","id":7}""")

        api.prevenirPlanta(7)
        assertEquals("POST /api/v1/plantas/7/prevenir", ruta)
        api.revivirPlanta(7)
        assertEquals("POST /api/v1/plantas/7/revivir", ruta)
        api.golpeVisto(7)
        assertEquals("POST /api/v1/plantas/7/golpe-visto", ruta)
    }
}
