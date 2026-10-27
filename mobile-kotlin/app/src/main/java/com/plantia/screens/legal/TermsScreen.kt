package com.plantia.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado

/**
 * TEXTO DE EJEMPLO: redactado para un proyecto academico, sin valor legal.
 * Cambiarlo por el texto definitivo si el proyecto sale del curso.
 */
private val SECCIONES = listOf(
    "Qué es Plantia" to
        "Plantia es un proyecto académico de Tecsup para identificar y cuidar plantas. No es un servicio comercial.",
    "Qué datos guardamos" to
        "Tu nombre, tu correo, tu contraseña (guardada cifrada) y las fotos de las plantas que escaneas.",
    "Para qué los usamos" to
        "Para tu cuenta, tu colección y tu jardín. No los vendemos ni los compartimos con terceros.",
    "Inteligencia artificial" to
        "Las fotos se envían a un servicio de IA externo para identificar la planta. Si el proyecto cambia de proveedor, este aviso sigue siendo válido.",
    "Identificación orientativa" to
        "La IA puede equivocarse. Antes de acercar una planta a una mascota o a un niño, confírmalo con un especialista, sobre todo su toxicidad.",
    "Tu cuenta" to
        "Puedes pedir que se borre tu cuenta. Te pedimos usar la app de forma responsable.",
    "Cambios" to
        "Estos términos pueden actualizarse durante el curso. Te avisaremos dentro de la app."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit = {}) {
    IconosOscurosEnBarraDeEstado()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Términos y privacidad", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Respeta la barra de navegacion y los recortes de pantalla al rotar.
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
                    ) {
                        Text("Entendido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Aviso de que es un texto de ejemplo.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmberLight)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = AmberAlert)
                Text(
                    "Texto de ejemplo para un proyecto académico, sin valor legal.",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }

            SECCIONES.forEachIndexed { indice, (titulo, texto) ->
                Seccion(numero = indice + 1, titulo = titulo, texto = texto)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Seccion(numero: Int, titulo: String, texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceWhite)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$numero",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = GreenDark,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(GreenLight)
                .padding(top = 5.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(texto, fontSize = 14.sp, lineHeight = 20.sp, color = TextSecondary)
        }
    }
}

@Preview(name = "Portrait", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewPortrait() {
    MobilekotlinTheme(dynamicColor = false) { TermsScreen() }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) { TermsScreen() }
}
