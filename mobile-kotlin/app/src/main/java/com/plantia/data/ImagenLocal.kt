package com.plantia.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Utilidades para trabajar con imagenes locales antes de subirlas.
 */
object ImagenLocal {

    /**
     * Copia el contenido de un Uri (galeria) a un archivo temporal en cache.
     * Devuelve el archivo y su tipo MIME, o null si no se pudo leer.
     */
    fun desdeUri(context: Context, uri: Uri): ImagenLocalPreparada? {
        return try {
            val mime = context.contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    uri.toString().substringAfterLast('.', "").lowercase()
                )
                ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            val destino = File(context.cacheDir, "plantia_galeria_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            } ?: return null
            if (destino.length() == 0L) return null
            ImagenLocalPreparada(destino, mime)
        } catch (e: Exception) {
            null
        }
    }
}

/** Archivo local listo para subir, con su tipo MIME. */
data class ImagenLocalPreparada(val archivo: File, val mimeType: String)
