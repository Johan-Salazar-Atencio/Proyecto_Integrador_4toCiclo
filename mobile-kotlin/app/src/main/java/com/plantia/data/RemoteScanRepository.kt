package com.plantia.data

import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.ScanResultDto
import com.plantia.model.RecentScan
import com.plantia.util.ImageCompressor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class RemoteScanRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : ScanRepository {

    private val ultimo = MutableStateFlow<ScanResultDto?>(null)
    override val ultimoResultado: StateFlow<ScanResultDto?> = ultimo

    override suspend fun escanear(foto: File): ScanResultDto {
        // Foto reducida: sube y se analiza bastante mas rapido que la original de la camara.
        val aSubir = withContext(Dispatchers.IO) { ImageCompressor.reducirParaSubida(foto) }
        val parte = MultipartBody.Part.createFormData(
            "foto",
            aSubir.name,
            aSubir.asRequestBody("image/jpeg".toMediaType())
        )
        // La primera vez RetrofitClient sondea el host por red: nunca en el hilo principal.
        val servicio = withContext(Dispatchers.IO) { api() }
        val resultado = servicio.escanear(Sesion.userId.value, parte)
        ultimo.value = resultado
        return resultado
    }

    override suspend fun recientes(): List<RecentScan> = withContext(Dispatchers.IO) {
        api().escaneosRecientes().body().orEmpty().map { it.toDomain() }
    }

    override fun limpiarUltimo() {
        ultimo.value = null
    }
}
