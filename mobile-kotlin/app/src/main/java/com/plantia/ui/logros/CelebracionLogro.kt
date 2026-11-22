package com.plantia.ui.logros

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.plantia.data.local.EstadoApp
import com.plantia.data.logros.AvisosLogros
import com.plantia.data.logros.GestorLogros
import com.plantia.data.model.LogroNuevo
import com.plantia.di.AppContainer
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Punto unico de enganche de la celebracion de un logro desbloqueado.
 *
 * Esta es la version provisional (un dialogo con el titulo y la recompensa). La animacion real se cambia
 * aqui dentro sin tocar nada mas: quien la llama solo le da el [logro] y espera a [onTerminado], que se llama
 * cuando la celebracion acaba (o el usuario la cierra). Solo entonces el logro se marca como visto.
 */
@Composable
fun CelebracionLogro(logro: LogroNuevo, onTerminado: () -> Unit) {
    val color = logro.rareza.color()
    val recompensa = logro.textoRecompensa()
    AlertDialog(
        onDismissRequest = onTerminado,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconoDeLogro(logro.icono), contentDescription = null, tint = SobrePrimario, modifier = Modifier.size(34.dp))
            }
        },
        title = {
            Text("¡Logro desbloqueado!", fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(logro.titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                Text(logro.rareza.etiqueta, fontSize = 12.sp, color = TextSecondary)
                if (logro.descripcion.isNotBlank()) {
                    Text(logro.descripcion, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                }
                if (recompensa.isNotEmpty()) {
                    Text(recompensa, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTerminado,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = SobrePrimario)
            ) { Text("¡Genial!", fontWeight = FontWeight.SemiBold) }
        }
    )
}

private fun Modifier.fillMaxWidth(): Modifier = this.then(Modifier.padding(horizontal = 0.dp))

/**
 * Muestra la celebracion del logro que toca, uno tras otro, y revisa los pendientes del servidor cada vez
 * que la app pasa a primer plano. Va una sola vez, encima de toda la navegacion.
 *
 * Despues de la ultima celebracion pide (una sola vez) el permiso de notificaciones para avisar de los logros
 * que se desbloqueen con la app cerrada.
 */
@Composable
fun HostCelebracionLogros(gestor: GestorLogros = AppContainer.celebracionLogros) {
    val contexto = LocalContext.current
    val actual by gestor.actual.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        EstadoApp.enPrimerPlano = true
        gestor.revisar()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { EstadoApp.enPrimerPlano = false }

    // Si lo niega no se vuelve a pedir: los logros se siguen celebrando dentro de la app
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    actual?.let { logro ->
        key(logro.idLogro) {
            CelebracionLogro(logro) {
                gestor.terminar(logro.idLogro)
                if (gestor.enCola == 0 && AvisosLogros.debePedirPermiso(contexto)) {
                    AvisosLogros.marcarPermisoPedido(contexto)
                    pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
