package com.plantia.screens.detail

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.plantia.data.GardenRepository
import com.plantia.data.PlantaResultado
import com.plantia.data.remote.ScanResultDto
import kotlinx.coroutines.launch

/**
 * Devuelve la accion "Digitalizar": crea la planta en el jardin y, si el servidor lo confirma,
 * llama a [alTerminar]. Si falla, avisa con un mensaje y no navega.
 */
@Composable
fun rememberDigitalizador(alTerminar: () -> Unit): (ScanResultDto) -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return { resultado ->
        scope.launch {
            when (val r = GardenRepository.digitalizar(resultado.idEscaneo, resultado.idCatalogo)) {
                is PlantaResultado.Con -> alTerminar()
                is PlantaResultado.Fallo ->
                    Toast.makeText(context, "No se pudo digitalizar: ${r.mensaje}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
