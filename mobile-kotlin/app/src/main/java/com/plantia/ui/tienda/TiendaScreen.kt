package com.plantia.ui.tienda

import androidx.compose.ui.graphics.Color
import com.plantia.ui.theme.FondoDegradado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.MaterialTheme
import com.plantia.ui.theme.AmberDark
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.tienda.viewmodel.TiendaUiState
import com.plantia.ui.tienda.viewmodel.TiendaViewModel
import com.plantia.util.IconosOscurosEnBarraDeEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendaScreen(
    onBack: () -> Unit = {},
    viewModel: TiendaViewModel = viewModel(factory = TiendaViewModel.Factory)
) {
    IconosOscurosEnBarraDeEstado()
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableStateOf("MACETA") }

    LaunchedEffect(viewModel.aviso) {
        viewModel.aviso?.let {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it)
            viewModel.avisoVisto()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(FondoDegradado),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            when (val estado = viewModel.uiState) {
                TiendaUiState.Loading -> CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.align(Alignment.Center))
                is TiendaUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("😕", fontSize = 48.sp)
                    Text("No se pudo abrir la tienda", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(estado.mensaje, fontSize = 14.sp, color = TextSecondary)
                    Button(onClick = viewModel::cargar, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Reintentar") }
                }
                is TiendaUiState.Listo -> Contenido(
                    tienda = estado.tienda,
                    pestana = pestana,
                    onPestana = { pestana = it },
                    ocupado = viewModel.ocupado,
                    onComprar = viewModel::comprar,
                    onAbono = viewModel::usarAbono,
                    onAplicar = viewModel::abrirAplicar
                )
            }
        }
    }

    viewModel.aplicando?.let { maceta ->
        HojaAplicarMaceta(
            maceta = maceta,
            plantas = viewModel.plantasParaMaceta,
            cargando = viewModel.cargandoPlantas,
            onAplicar = viewModel::aplicarMaceta,
            onCerrar = viewModel::cerrarAplicar
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contenido(
    tienda: Tienda,
    pestana: String,
    onPestana: (String) -> Unit,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit,
    onAbono: (TiendaItem) -> Unit,
    onAplicar: (TiendaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tienda", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                androidx.compose.material3.Surface(shape = RoundedCornerShape(20.dp), color = SurfaceWhite, shadowElevation = 2.dp) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Eco, contentDescription = "Hojas", tint = com.plantia.ui.theme.GreenDark, modifier = Modifier.size(16.dp))
                        Text("${tienda.monedas}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberDark)
                    }
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("ABONO" to "Abono", "FONDO" to "Fondos", "MACETA" to "Macetas").forEach { (clave, texto) ->
                    val elegida = pestana == clave
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (elegida) GreenPrimary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onPestana(clave) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            texto,
                            fontSize = 13.sp,
                            fontWeight = if (elegida) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (elegida) androidx.compose.ui.graphics.Color.White else TextSecondary
                        )
                    }
                }
            }
        }
        if (pestana == "MACETA") {
            if (tienda.macetas.isEmpty()) {
                item { Text("Aún no hay macetas a la venta.", color = TextSecondary, fontSize = 14.sp) }
            }
            items(tienda.macetas.chunked(2), key = { par -> "m${par.first().id}" }) { par ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    par.forEach { maceta ->
                        TarjetaMaceta(maceta, tienda.monedas, ocupado, onComprar, onAplicar, Modifier.weight(1f))
                    }
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else if (pestana == "FONDO") {
            if (tienda.invernaderos.isNotEmpty()) {
                item { Text("Invernaderos de tu colección", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
                items(tienda.invernaderos, key = { "i${it.id}" }) { invernadero ->
                    TarjetaInvernadero(invernadero, tienda.monedas, ocupado, onComprar)
                }
            }
            item { Text("Fondos de la escena de cada planta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
            if (tienda.fondos.isEmpty()) {
                item { Text("Aún no hay fondos a la venta.", color = TextSecondary, fontSize = 14.sp) }
            }
            items(tienda.fondos, key = { it.id }) { fondo ->
                TarjetaFondo(fondo, tienda.monedas, ocupado, onComprar)
            }
        } else {
            if (tienda.abonos.isEmpty()) {
                item { Text("El abono no está disponible por ahora.", color = TextSecondary, fontSize = 14.sp) }
            }
            items(tienda.abonos, key = { "a${it.id}" }) { abono ->
                TarjetaAbono(abono, tienda.monedas, ocupado) { onAbono(abono) }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TarjetaFondo(
    fondo: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EscenasDelTema(fondo.variantes)
            Text(fondo.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(fondo.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
            when {
                fondo.comprado ->
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text("✓ Lo tienes · elígelo en el icono de fondos del jardín")
                    }
                fondo.bloqueado ->
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("🔒 Nivel ${fondo.nivelRequerido}") }
                else ->
                    Button(onClick = { onComprar(fondo) }, enabled = !ocupado && monedas >= fondo.precio, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Comprar · ${fondo.precio} 🍃") }
            }
        }
    }
}

@Composable
private fun TarjetaAbono(abono: TiendaItem, monedas: Int, ocupado: Boolean, onAbono: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(GreenLight), contentAlignment = Alignment.Center) { Text("🌿", fontSize = 28.sp) }
                Column {
                    Text(abono.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(abono.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
                }
            }
            Button(
                onClick = onAbono,
                enabled = !ocupado && !abono.bloqueado && monedas >= abono.precio,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text(if (abono.bloqueado) "🔒 Nivel ${abono.nivelRequerido}" else "Usar en mi planta · ${abono.precio} 🍃") }
            Text("Se aplica a la planta de tu jardín. Máximo un abono al día por planta.", fontSize = 12.sp, color = TextSecondary)
        }
    }
}
