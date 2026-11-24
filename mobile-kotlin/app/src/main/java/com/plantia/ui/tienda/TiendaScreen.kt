package com.plantia.ui.tienda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.Abono
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.FondoDegradado
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.tienda.viewmodel.TiendaUiState
import com.plantia.ui.tienda.viewmodel.TiendaViewModel
import com.plantia.util.IconosOscurosEnBarraDeEstado

private const val CAT_MACETA = "MACETA"
private const val CAT_INVERNADERO = "INVERNADERO"
private const val CAT_FONDO = "FONDO"
private const val CAT_ABONO = "ABONO"

@Composable
fun TiendaScreen(
    onBack: () -> Unit = {},
    viewModel: TiendaViewModel = viewModel(factory = TiendaViewModel.Factory)
) {
    // QA-64: antes "Comprar" cobraba al primer toque, sin confirmar
    var porComprar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.plantia.data.model.TiendaItem?>(null) }
    IconosOscurosEnBarraDeEstado()
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableStateOf(CAT_MACETA) }

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
                TiendaUiState.Loading -> CircularProgressIndicator(color = Primario, modifier = Modifier.align(Alignment.Center))
                is TiendaUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("😕", fontSize = 48.sp)
                    Text("No se pudo abrir la tienda", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(estado.mensaje, fontSize = 14.sp, color = TextSecondary)
                    Button(
                        onClick = viewModel::cargar,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario)
                    ) { Text("Reintentar") }
                }
                is TiendaUiState.Listo -> Contenido(
                    tienda = estado.tienda,
                    pestana = pestana,
                    onPestana = { pestana = it },
                    ocupado = viewModel.ocupado,
                    onComprar = { porComprar = it },
                    onAbono = viewModel::usarAbono,
                    onAplicar = viewModel::abrirAplicar
                )
            }
        }
    }

    porComprar?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { porComprar = null },
            title = { Text(TextosCompra.titulo(item.nombre)) },
            text = { Text(TextosCompra.detalle(item.precio)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { porComprar = null; viewModel.comprar(item) }) {
                    Text("Comprar", color = Primario, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { porComprar = null }) { Text("Cancelar") }
            }
        )
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
    val horizontal = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val categorias = listOf(
            CAT_MACETA to "Macetas",
            CAT_INVERNADERO to "Invernaderos",
            CAT_FONDO to "Fondos",
            CAT_ABONO to "Abono"
        )
        if (horizontal) {
            // En horizontal sobra ancho y falta alto: titulo, categorias y hojas van en una sola fila
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tienda", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categorias.forEach { (clave, texto) -> ChipCategoria(texto, pestana == clave) { onPestana(clave) } }
                    }
                    PildoraHojas(tienda.monedas)
                }
            }
        } else {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tienda", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    PildoraHojas(tienda.monedas)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categorias.forEach { (clave, texto) -> ChipCategoria(texto, pestana == clave) { onPestana(clave) } }
                }
            }
        }
        item {
            Text(
                when (pestana) {
                    CAT_MACETA -> "Las macetas se ven en tu colección y bajo tu planta."
                    CAT_INVERNADERO -> "El invernadero es el fondo de tu colección. Elige el que quieras desde su píldora."
                    CAT_FONDO -> "Los fondos son la escena que se ve detrás de cada planta en tu jardín."
                    else -> "El abono se aplica a la planta de tu jardín. Máximo uno al día por planta."
                },
                fontSize = 11.5.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
        when (pestana) {
            CAT_MACETA -> {
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
            }
            CAT_INVERNADERO -> {
                if (tienda.invernaderos.isEmpty()) {
                    item { Text("Aún no hay invernaderos a la venta.", color = TextSecondary, fontSize = 14.sp) }
                }
                items(tienda.invernaderos, key = { "i${it.id}" }) { invernadero ->
                    TarjetaInvernadero(invernadero, tienda.monedas, ocupado, onComprar)
                }
            }
            CAT_FONDO -> {
                if (tienda.fondos.isEmpty()) {
                    item { Text("Aún no hay fondos a la venta.", color = TextSecondary, fontSize = 14.sp) }
                }
                items(tienda.fondos, key = { it.id }) { fondo ->
                    TarjetaFondo(fondo, tienda.monedas, ocupado, onComprar)
                }
            }
            else -> {
                if (tienda.abonos.isEmpty()) {
                    item { Text("El abono no está disponible por ahora.", color = TextSecondary, fontSize = 14.sp) }
                }
                items(tienda.abonos, key = { "a${it.id}" }) { abono ->
                    TarjetaAbono(abono, tienda.monedas, ocupado) { onAbono(abono) }
                }
            }
        }
    }
}

/** Pildora con las hojas del usuario. */
@Composable
private fun PildoraHojas(hojas: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(SuperficieElevada)
            .border(1.dp, DividerColor, RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Eco, contentDescription = "Hojas", tint = Primario, modifier = Modifier.size(16.dp))
        Text("$hojas", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
    }
}

@Composable
private fun ChipCategoria(texto: String, activo: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Text(
        texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (activo) SobrePrimario else TextSecondary,
        maxLines = 1,
        modifier = Modifier
            .clip(forma)
            .background(if (activo) Primario else com.plantia.ui.theme.SurfaceWhite)
            .border(1.dp, if (activo) Primario else DividerColor, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    )
}

@Composable
private fun TarjetaFondo(
    fondo: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().tarjeta().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EscenasDelTema(fondo.variantes)
        Text(fondo.nombre, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text(fondo.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
        when {
            fondo.comprado ->
                Text("Lo tienes · elígelo en el icono de fondos del jardín", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Primario)
            fondo.bloqueado -> BotonBloqueado(fondo.nivelRequerido)
            else -> BotonComprar(fondo.precio, enabled = !ocupado && monedas >= fondo.precio, modifier = Modifier.fillMaxWidth()) { onComprar(fondo) }
        }
    }
}

@Composable
private fun TarjetaAbono(abono: TiendaItem, monedas: Int, ocupado: Boolean, onAbono: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().tarjeta().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Abono.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text("🌿", fontSize = 26.sp)
            }
            Column {
                Text(abono.nombre, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(abono.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
            }
        }
        if (abono.bloqueado) {
            BotonBloqueado(abono.nivelRequerido)
        } else {
            Button(
                onClick = onAbono,
                enabled = !ocupado && monedas >= abono.precio,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario)
            ) {
                Text("Usar en mi planta · ${abono.precio}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.size(5.dp))
                Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** "Comprar · N" con la hoja al lado: el boton primario de la tienda. */
@Composable
internal fun BotonComprar(precio: Int, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text("Comprar · $precio", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Spacer(Modifier.size(5.dp))
        Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(14.dp))
    }
}

/** Boton apagado con candado cuando todavia no se alcanza el nivel. */
@Composable
internal fun BotonBloqueado(nivel: Int, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {},
        enabled = false,
        modifier = modifier.fillMaxWidth().height(36.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(disabledContainerColor = SuperficieElevada, disabledContentColor = TextSecondary),
        border = null,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(5.dp))
        Text("Nivel $nivel", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Textos de la confirmacion de compra (QA-64). */
internal object TextosCompra {
    fun titulo(nombre: String) = "¿Comprar $nombre?"
    fun detalle(precio: Int) = "Se descuentan $precio hojas de tu saldo."
}
