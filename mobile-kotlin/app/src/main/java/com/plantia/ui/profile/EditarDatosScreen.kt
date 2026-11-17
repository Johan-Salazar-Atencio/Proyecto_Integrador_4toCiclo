package com.plantia.ui.profile

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.plantia.ui.theme.FondoDegradado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.ui.profile.viewmodel.EditarDatosViewModel
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Editar datos: el usuario cambia su nombre; el correo se muestra y no se edita. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarDatosScreen(
    onBack: () -> Unit,
    viewModel: EditarDatosViewModel = viewModel(factory = EditarDatosViewModel.Factory)
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.aviso) {
        viewModel.aviso?.let {
            snackbar.showSnackbar(it)
            viewModel.avisoMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar datos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent,
        modifier = Modifier.background(FondoDegradado)
    ) { padding ->
        if (viewModel.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilaEditable(
                titulo = "Tu nombre",
                inicial = viewModel.nombreGuardado.orEmpty(),
                etiqueta = "Nombre",
                onGuardar = viewModel::guardarNombre
            )
            FilaSoloLectura(
                titulo = "Tu correo",
                valor = viewModel.correo.ifBlank { "—" },
                nota = "El correo no se puede cambiar."
            )
            Text(
                "El apodo de cada planta se cambia desde la planta, en tu jardín.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

/** Un dato que el usuario puede ver pero no editar (el correo). */
@Composable
private fun FilaSoloLectura(titulo: String, valor: String, nota: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, fontSize = 12.sp, color = TextSecondary)
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(nota, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

/** Un campo de texto con su boton Guardar, que solo se activa cuando el texto cambió. */
@Composable
private fun FilaEditable(titulo: String, inicial: String, etiqueta: String, onGuardar: (String) -> Unit) {
    var texto by remember(inicial) { mutableStateOf(inicial) }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, fontSize = 12.sp, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(100) },
                    label = { Text(etiqueta) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onGuardar(texto) },
                    enabled = texto.trim() != inicial.trim(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = MaterialTheme.colorScheme.surface)
                ) { Text("Guardar") }
            }
        }
    }
}
