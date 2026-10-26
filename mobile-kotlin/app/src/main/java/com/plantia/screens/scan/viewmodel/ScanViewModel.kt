package com.plantia.screens.scan.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.ScanResultDto
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val result: ScanResultDto) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel : ViewModel() {

    var uiState: ScanUiState by mutableStateOf(ScanUiState.Idle)
        private set

    private var trabajo: Job? = null

    /** Captura tomada -> se sube al backend y se espera la informacion de la planta. */
    fun escanear(foto: File) {
        uiState = ScanUiState.Loading
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            try {
                // Foto reducida: sube y se analiza bastante mas rapido que la original de la camara.
                val aSubir = withContext(Dispatchers.IO) { reducirParaSubida(foto) }
                val part = MultipartBody.Part.createFormData(
                    "foto",
                    aSubir.name,
                    aSubir.asRequestBody("image/jpeg".toMediaType())
                )
                // La primera vez RetrofitClient sondea el host por red: nunca en el hilo principal.
                val api = withContext(Dispatchers.IO) { RetrofitClient.api }
                val result = api.escanear(USER_ID, part)
                uiState = ScanUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = ScanUiState.Error(e.message ?: "No se pudo conectar con el servidor")
            }
        }
    }

    /** Cancela la busqueda en curso y vuelve a la camara. */
    fun cancelar() {
        trabajo?.cancel()
        trabajo = null
        uiState = ScanUiState.Idle
    }

    fun reiniciar() {
        uiState = ScanUiState.Idle
    }

    /**
     * Reduce el lado mayor a [maxLado] px y recomprime en JPEG, respetando la orientacion EXIF.
     * Si la foto ya es pequena, se sube tal cual.
     */
    private fun reducirParaSubida(foto: File, maxLado: Int = 1280): File {
        val bordes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(foto.absolutePath, bordes)
        val mayor = max(bordes.outWidth, bordes.outHeight)
        if (mayor <= 0 || (mayor <= maxLado && foto.length() < 1_000_000)) return foto

        var muestra = 1
        while (mayor / (muestra * 2) >= maxLado) muestra *= 2
        val original = BitmapFactory.decodeFile(
            foto.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = muestra }
        ) ?: return foto

        val giro = when (
            ExifInterface(foto.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = maxLado.toFloat() / max(original.width, original.height)
        val matriz = Matrix().apply {
            if (escala < 1f) postScale(escala, escala)
            if (giro != 0f) postRotate(giro)
        }
        val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)

        val destino = File(foto.parentFile, "subida_${foto.name}")
        destino.outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (final !== original) original.recycle()
        final.recycle()
        return destino
    }

    companion object {
        /** Usuario temporal (sin auth) hasta integrar login. */
        const val USER_ID = 1
    }
}
