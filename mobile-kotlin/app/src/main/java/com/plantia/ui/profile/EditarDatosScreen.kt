package com.plantia.ui.profile

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

/** Editar datos: el nombre del usuario y el nombre de cada una de sus plantas. */
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
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
            Text("Mis plantas", fontWeight = FontWeight.Bold, color = TextPrimary)
            if (viewModel.plantas.isEmpty()) {
                Text("Todavía no tienes plantas virtuales.", color = TextSecondary)
            }
            viewModel.plantas.forEach { planta ->
                FilaEditable(
                    titulo = planta.species,
                    inicial = planta.name,
                    etiqueta = "Nombre de la planta",
                    onGuardar = { viewModel.guardarPlanta(planta, it) }
                )
            }
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
