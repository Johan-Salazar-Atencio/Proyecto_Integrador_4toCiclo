package com.plantia.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
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
    fun prevenir_revivir_y_golpe_visto_usan_su_ruta() = runTest {
        val api = api("""{"name":"Monstera","id":7}""")

        api.prevenirPlanta(7)
        assertEquals("POST /api/v1/plantas/7/prevenir", ruta)
        api.revivirPlanta(7)
        assertEquals("POST /api/v1/plantas/7/revivir", ruta)
        api.golpeVisto(7)
        assertEquals("POST /api/v1/plantas/7/golpe-visto", ruta)
    }

    @Test
    fun baul_apodo_y_macetas_usan_su_ruta_y_su_cuerpo() = runTest {
        val una = api("""{"name":"Fifi","id":7,"enBaul":true,"macetaCodigo":"AZUL"}""")

        una.guardarEnBaul(7)
        assertEquals("POST /api/v1/plantas/7/baul", ruta)
        una.sacarDelBaul(7)
        assertEquals("POST /api/v1/plantas/7/sacar-del-baul", ruta)
        una.renombrarPlanta(7, com.plantia.data.remote.dto.ApodoRequest("Fifi"))
        assertEquals("PUT /api/v1/plantas/7/apodo", ruta)
        assertEquals("""{"apodo":"Fifi"}""", cuerpo)
        val plantas = api("""[{"name":"Fifi","id":7,"enBaul":false,"macetaCodigo":"AZUL"}]""")
        val respuesta = plantas.ponerMacetas(com.plantia.data.remote.dto.PonerMacetaRequest(10, listOf(7, 8)))
        assertEquals("PUT /api/v1/plantas/macetas", ruta)
        assertEquals("""{"idItem":10,"ids":[7,8]}""", cuerpo)
        assertEquals("AZUL", respuesta.body()?.first()?.macetaCodigo)
    }

    @Test
    fun invernadero_y_foto_de_perfil_usan_su_ruta() = runTest {
        val api = api("""{"name":"Johan","greenhouseCode":"VICTORIANO"}""")

        val perfil = api.cambiarInvernadero(com.plantia.data.remote.dto.ElegirInvernaderoRequest(15))
        assertEquals("PUT /api/v1/usuarios/me/invernadero", ruta)
        assertEquals("""{"idItem":15}""", cuerpo)
        assertEquals("VICTORIANO", perfil.body()?.greenhouseCode)
        val parte = okhttp3.MultipartBody.Part.createFormData(
            "foto", "yo.jpg", okhttp3.RequestBody.create("image/jpeg".toMediaType(), byteArrayOf(1, 2))
        )
        api.subirFoto(parte)
        assertEquals("POST /api/v1/usuarios/me/foto", ruta)
    }
}
