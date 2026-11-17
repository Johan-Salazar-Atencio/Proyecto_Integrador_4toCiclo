package com.plantia.ui.home.components

import com.plantia.ui.components.brilloTarjeta

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.plantia.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** Consejos que van pasando en el Inicio, como los de las pantallas de carga de un juego. */
internal val ConsejosDelInicio = listOf(
    "Si hay una ola de calor, refresca bien a tus plantas: riégalas temprano o al atardecer.",
    "Una helada puede quemar las hojas: cubre tus plantas por la noche.",
    "Revisa las hojas todos los días: detectar una plaga a tiempo salva a la planta.",
    "Regar de más también daña: deja que la tierra se seque un poco entre riegos.",
    "Cubrir la tierra con paja o rastrojo ayuda a conservar la humedad.",
    "Los huaycos arrastran terrenos de cultivo: las zanjas desvían el agua y el lodo.",
    "Una planta con buena luz y buena agua mantiene su salud alta.",
    "Un abono a tiempo ayuda a que tu planta crezca más rápido.",
    "Escanear una especie nueva da más hojas y XP que repetir una ya vista.",
    "Algunas plantas son tóxicas para las mascotas: revisa su ficha antes de dejarlas al alcance."
)

/** Escenas tranquilas del campo peruano (ilustraciones hechas con IA): dia, lluvia en los andenes, atardecer y noche. */
private val EscenasSinEvento = listOf(
    R.drawable.sin_evento_1,
    R.drawable.sin_evento_2,
    R.drawable.sin_evento_3,
    R.drawable.sin_evento_4
)

/** Paginas "infinitas": se arranca en el medio para poder deslizar hacia ambos lados. */
private const val PAGINAS_CARRUSEL = 10_000

/**
 * Lo que se ve en el Inicio cuando no hay ningun evento: un carrusel de escenas del campo que pasa solo
 * (tambien se puede deslizar), un aviso tranquilo y un consejo que cambia con cada escena, para que la
 * pantalla no quede vacia ni estatica.
 */
@Composable
fun SinEventoCard(modifier: Modifier = Modifier, consejos: List<String> = ConsejosDelInicio, cadaMs: Long = 6_000L) {
    val inicio = PAGINAS_CARRUSEL / 2 - (PAGINAS_CARRUSEL / 2) % EscenasSinEvento.size
    val pager = rememberPagerState(initialPage = inicio) { PAGINAS_CARRUSEL }
    // Avanza solo; si el usuario desliza, el conteo vuelve a empezar desde esa escena
    LaunchedEffect(pager.settledPage) {
        delay(cadaMs)
        pager.animateScrollToPage(pager.currentPage + 1)
    }
    val paso = pager.currentPage - inicio
    val consejo = if (consejos.isEmpty()) "" else consejos[Math.floorMod(paso, consejos.size)]

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).brilloTarjeta(GreenPrimary, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { pagina ->
                Image(
                    painter = painterResource(EscenasSinEvento[Math.floorMod(pagina, EscenasSinEvento.size)]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Puntos: en que escena va el carrusel
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val actual = Math.floorMod(pager.currentPage, EscenasSinEvento.size)
                EscenasSinEvento.indices.forEach { k ->
                    Box(
                        Modifier
                            .size(if (k == actual) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (k == actual) Color.White else Color.White.copy(alpha = 0.55f))
                    )
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text("No hay eventos por el momento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "Sigue cuidando tus plantas y mantente alerta a los posibles eventos.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenLight)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = GreenDark, modifier = Modifier.size(20.dp))
                AnimatedContent(
                    targetState = consejo,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "consejo",
                    modifier = Modifier.weight(1f)
                ) { texto ->
                    Text(texto, fontSize = 13.sp, lineHeight = 18.sp, color = TextPrimary, modifier = Modifier.height(54.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** Inicio sin plantas: en vez de quedar vacio, invita a escanear la primera. */
@Composable
fun SinPlantasCard(onScan: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🪴", fontSize = 48.sp)
            Text("Tu jardín está esperando", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Escanea tu primera planta para identificarla y empezar a cuidarla.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
            ) { Text("Escanear una planta", fontWeight = FontWeight.SemiBold) }
        }
    }
    Spacer(Modifier.height(12.dp))
}
