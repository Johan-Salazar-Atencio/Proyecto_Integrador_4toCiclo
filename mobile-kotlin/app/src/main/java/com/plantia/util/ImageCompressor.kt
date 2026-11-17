package com.plantia.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import kotlin.math.max

object ImageCompressor {

    /**
     * Reduce el lado mayor a [maxLado] px y recomprime en JPEG, respetando la orientacion EXIF.
     * Si la foto ya es pequena, se devuelve tal cual.
     */
    fun reducirParaSubida(foto: File, maxLado: Int = 1280): File {
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
}
