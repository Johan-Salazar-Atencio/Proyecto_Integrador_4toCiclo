package com.plantia.data.remote

import com.plantia.data.mapper.logrosNuevos
import com.plantia.data.mapper.toDomain
import com.plantia.data.model.Rareza
import com.plantia.data.remote.dto.LogrosVistosRequest
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Rutas y lectura de los logros con un servidor falso, igual que PlantiaApiTest. */
class LogrosApiTest {

    private var ruta = ""
    private var cuerpo = ""

    private fun api(respuesta: String, codigo: Int = 200): PlantiaApi {
        val cliente = OkHttpClient.Builder().addInterceptor { cadena ->
            val peticion = cadena.request()
            ruta = "${peticion.method} ${peticion.url.encodedPath}"
            cuerpo = peticion.body?.let { Buffer().also { b -> it.writeTo(b) }.readUtf8() }.orEmpty()
            Response.Builder()
                .request(peticion).protocol(Protocol.HTTP_1_1).code(codigo).message("OK")
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
    fun lista_de_logros_usa_su_ruta_y_se_lee_completa() = runTest {
        val api = api(
            """{"total":2,"desbloqueados":1,"logros":[
              {"idLogro":1,"codigo":"PRIMERA_HOJA","titulo":"Primera hoja","descripcion":"d","icono":"hoja","rareza":"RARA",
               "meta":1,"progreso":1,"desbloqueado":true,"desbloqueadoEn":"2026-11-20T10:15:30","secreto":false,
               "recompensaXp":20,"recompensaMonedas":5},
              {"idLogro":2,"codigo":"X","titulo":"???","descripcion":"Logro secreto","icono":"secreto","rareza":"LEGENDARIA",
               "meta":5,"progreso":2,"desbloqueado":false,"secreto":true,"recompensaXp":0,"recompensaMonedas":0}]}"""
        )

        val resumen = api.logros().body()!!.toDomain()

        assertEquals("GET /api/v1/logros", ruta)
        assertEquals(2, resumen.total)
        assertEquals(1, resumen.desbloqueados)
        assertEquals(Rareza.RARA, resumen.logros[0].rareza)
        assertEquals("2026-11-20T10:15:30", resumen.logros[0].desbloqueadoEn)
        assertNull(resumen.logros[1].desbloqueadoEn)
        assertTrue(resumen.logros[1].oculto)
    }

    @Test
    fun pendientes_y_vistos_usan_su_ruta_y_su_cuerpo() = runTest {
        val pendientes = api("""[{"idLogro":4,"codigo":"A","titulo":"T","descripcion":"d","icono":"sol","rareza":"EPICA","recompensaXp":10,"recompensaMonedas":2}]""")
            .logrosPendientes().body()!!
        assertEquals("GET /api/v1/logros/pendientes", ruta)
        assertEquals(Rareza.EPICA, pendientes[0].toDomain().rareza)

        val vacia = api("[]").logrosPendientes().body()!!
        assertTrue(vacia.isEmpty())

        val respuesta = api("", codigo = 204).logrosVistos(LogrosVistosRequest(listOf(4, 9)))
        assertEquals("POST /api/v1/logros/vistos", ruta)
        assertEquals("""{"ids":[4,9]}""", cuerpo)
        assertTrue(respuesta.isSuccessful)
    }

    @Test
    fun el_progreso_trae_los_logros_nuevos_si_el_servidor_los_manda() = runTest {
        val api = api(
            """{"name":"Monstera","id":7,"progreso":{"xpGanado":10,"monedasGanadas":1,"nivel":2,"subioDeNivel":false,
               "experienciaXp":30,"rachaDias":1,"logrosNuevos":[
               {"idLogro":3,"codigo":"C","titulo":"Racha","descripcion":"d","icono":"racha","rareza":"COMUN","recompensaXp":5,"recompensaMonedas":0}]}}"""
        )

        val planta = api.cuidarPlanta(7, mapOf("accion" to "REGAR")).body()!!

        assertEquals(listOf(3), planta.progreso.logrosNuevos().map { it.idLogro })
    }

    @Test
    fun un_servidor_viejo_sin_el_campo_no_rompe_la_lectura() = runTest {
        // Sin "progreso", con "progreso" sin logros y con "logrosNuevos": null
        val sinProgreso = api("""{"name":"Monstera","id":7}""").cuidarPlanta(7, mapOf("accion" to "REGAR")).body()!!
        assertTrue(sinProgreso.progreso.logrosNuevos().isEmpty())

        val sinLogros = api("""{"name":"Monstera","id":7,"progreso":{"xpGanado":10,"nivel":1}}""")
            .cuidarPlanta(7, mapOf("accion" to "REGAR")).body()!!
        assertTrue(sinLogros.progreso.logrosNuevos().isEmpty())

        val nulo = api("""{"name":"Monstera","id":7,"progreso":{"logrosNuevos":null}}""")
            .cuidarPlanta(7, mapOf("accion" to "REGAR")).body()!!
        assertTrue(nulo.progreso.logrosNuevos().isEmpty())
    }
}
