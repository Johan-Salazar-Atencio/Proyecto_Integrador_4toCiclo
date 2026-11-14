package com.plantia.ui.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.auth.viewmodel.ModoAuth
import com.plantia.ui.components.escalaAlPresionar
import com.plantia.ui.theme.CampoFondo
import com.plantia.ui.theme.FondoDegradado
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

internal val FondoAuth: Brush
    @Composable get() = FondoDegradado
internal val FondoCampo: Color
    @Composable get() = CampoFondo
internal val RojoError = Color(0xFFEF4444)
internal val RojoErrorClaro: Color
    @Composable get() = RojoClaro

/** Selector "Iniciar sesion / Registrarse" con la pestana activa en verde. */
@Composable
internal fun SelectorModo(modo: ModoAuth, alElegir: (ModoAuth) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(FondoCampo)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Pestana("Iniciar sesión", modo == ModoAuth.LOGIN, { alElegir(ModoAuth.LOGIN) }, Modifier.weight(1f))
        Pestana("Registrarse", modo == ModoAuth.REGISTRO, { alElegir(ModoAuth.REGISTRO) }, Modifier.weight(1f))
    }
}

@Composable
internal fun Pestana(texto: String, activa: Boolean, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    val fondo by animateColorAsState(if (activa) GreenPrimary else Color.Transparent, label = "fondo_pestana")
    val color by animateColorAsState(if (activa) Color.White else TextSecondary, label = "color_pestana")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .clickable(onClick = alPulsar)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
internal fun Campo(
    etiqueta: String,
    pista: String,
    valor: String,
    alCambiar: (String) -> Unit,
    icono: ImageVector,
    error: String?,
    teclado: KeyboardType,
    ime: ImeAction,
    alSiguiente: () -> Unit = {},
    alTerminar: () -> Unit = {},
    esPassword: Boolean = false,
    verPassword: Boolean = false,
    alAlternarPassword: (() -> Unit)? = null,
    habilitado: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado,
            singleLine = true,
            isError = error != null,
            placeholder = { Text(pista, color = TextSecondary.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(icono, contentDescription = null, tint = if (error != null) RojoError else GreenDark) },
            trailingIcon = if (esPassword && alAlternarPassword != null) {
                {
                    IconButton(onClick = alAlternarPassword) {
                        Icon(
                            if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = TextSecondary
                        )
                    }
                }
            } else null,
            visualTransformation = if (esPassword && !verPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ime),
            keyboardActions = KeyboardActions(onNext = { alSiguiente() }, onDone = { alTerminar() }),
            supportingText = error?.let { { Text(it, color = RojoError) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextSecondary,
                focusedContainerColor = FondoCampo,
                unfocusedContainerColor = FondoCampo,
                disabledContainerColor = FondoCampo,
                errorContainerColor = FondoCampo,
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = RojoError,
                cursorColor = GreenPrimary
            )
        )
    }
}

@Composable
internal fun CasillaTerminos(marcada: Boolean, alCambiar: (Boolean) -> Unit, alAbrirTerminos: () -> Unit) {
    val texto = buildAnnotatedString {
        append("Acepto los ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "terminos",
                styles = TextLinkStyles(
                    SpanStyle(color = GreenDark, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                ),
                linkInteractionListener = { alAbrirTerminos() }
            )
        ) { append("Términos y privacidad") }
        append(" de Plantia.")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = marcada,
            onCheckedChange = alCambiar,
            colors = CheckboxDefaults.colors(checkedColor = GreenPrimary, checkmarkColor = Color.White)
        )
        Text(texto, fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary)
    }
}

@Composable
internal fun BannerError(mensaje: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RojoErrorClaro)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RojoError)
        Text(mensaje, fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
internal fun BotonPrincipal(texto: String, habilitado: Boolean, cargando: Boolean, alPulsar: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        interactionSource = interaccion,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .escalaAlPresionar(interaccion),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = GreenPrimary,
            contentColor = Color.White,
            disabledContainerColor = GreenPrimary.copy(alpha = 0.35f),
            disabledContentColor = Color.White
        )
    ) {
        if (cargando) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
