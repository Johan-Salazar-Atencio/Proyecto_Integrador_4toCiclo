package com.plantia.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.ui.components.EscenaBrote
import com.plantia.ui.components.escalaAlPresionar
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.theme.FondoDegradado
import com.plantia.ui.theme.CampoFondo
import com.plantia.ui.theme.RojoClaro
import com.plantia.util.IconosOscurosEnBarraDeEstado

private val FondoAuth: Brush
    @Composable get() = FondoDegradado
private val FondoCampo: Color
    @Composable get() = CampoFondo
private val RojoError = Color(0xFFEF4444)
private val RojoErrorClaro: Color
    @Composable get() = RojoClaro

/**
 * Inicio de sesion y registro en una sola pantalla con pestanas.
 * [modoInicial] es "login" o "registro" (viene de Welcome).
 */
@Composable
fun AuthScreen(
    modoInicial: String? = null,
    onBack: () -> Unit = {},
    onTerminos: () -> Unit = {},
    onAutenticado: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    IconosOscurosEnBarraDeEstado()

    LaunchedEffect(modoInicial) {
        viewModel.aplicarModoInicial(if (modoInicial == "registro") ModoAuth.REGISTRO else ModoAuth.LOGIN)
    }
    // Exito: se entra a la app y se descarta esta pantalla de la pila.
    LaunchedEffect(viewModel.estado) {
        if (viewModel.estado is AuthEstado.Exito) {
            viewModel.exitoConsumido()
            onAutenticado()
        }
    }

    val config = LocalConfiguration.current
    val apaisado = config.screenWidthDp > config.screenHeightDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoAuth)
            // `safeDrawing` incluye el teclado: el formulario sube sin quedar tapado.
            .safeDrawingPadding()
    ) {
        if (apaisado) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.8f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Encabezado(viewModel.modo, tamanoEscena = 110.dp)
                }
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Formulario(viewModel, onTerminos)
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))
                Encabezado(viewModel.modo, tamanoEscena = 104.dp)
                Spacer(Modifier.height(20.dp))
                Formulario(viewModel, onTerminos, Modifier.widthIn(max = 520.dp))
                Spacer(Modifier.height(24.dp))
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
        }
    }
}

@Composable
private fun Encabezado(modo: ModoAuth, tamanoEscena: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EscenaBrote(modifier = Modifier.size(tamanoEscena), respira = false)
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (modo == ModoAuth.LOGIN) "¡Bienvenido a Plantia!" else "¡Únete a Plantia!",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (modo == ModoAuth.LOGIN) {
                "Escanea, aprende y cría tus plantas virtuales"
            } else {
                "Crea tu cuenta y empieza tu colección"
            },
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Formulario(viewModel: AuthViewModel, onTerminos: () -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    var verPassword by remember { mutableStateOf(false) }
    val registro = viewModel.modo == ModoAuth.REGISTRO
    val cargando = viewModel.estado is AuthEstado.Cargando

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SelectorModo(viewModel.modo, viewModel::elegirModo)

            AnimatedVisibility(
                visible = registro,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Campo(
                    etiqueta = "Nombre de jardinero",
                    pista = "Ej. Juan Pérez",
                    valor = viewModel.nombre,
                    alCambiar = viewModel::cambiarNombre,
                    icono = Icons.Filled.Person,
                    error = viewModel.errorNombre,
                    teclado = KeyboardType.Text,
                    ime = ImeAction.Next,
                    alSiguiente = { focus.moveFocus(FocusDirection.Down) },
                    habilitado = !cargando
                )
            }

            Campo(
                etiqueta = "Correo electrónico",
                pista = "tu@correo.com",
                valor = viewModel.email,
                alCambiar = viewModel::cambiarEmail,
                icono = Icons.Filled.Email,
                error = viewModel.errorEmail,
                teclado = KeyboardType.Email,
                ime = ImeAction.Next,
                alSiguiente = { focus.moveFocus(FocusDirection.Down) },
                habilitado = !cargando
            )

            Campo(
                etiqueta = "Contraseña",
                pista = if (registro) "Mínimo 8 caracteres" else "Tu contraseña",
                valor = viewModel.password,
                alCambiar = viewModel::cambiarPassword,
                icono = Icons.Filled.Lock,
                error = viewModel.errorPassword,
                teclado = KeyboardType.Password,
                ime = if (registro) ImeAction.Next else ImeAction.Done,
                alSiguiente = { focus.moveFocus(FocusDirection.Down) },
                alTerminar = { focus.clearFocus(); viewModel.enviar() },
                esPassword = true,
                verPassword = verPassword,
                alAlternarPassword = { verPassword = !verPassword },
                habilitado = !cargando
            )

            AnimatedVisibility(
                visible = registro,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Campo(
                        etiqueta = "Confirmar contraseña",
                        pista = "Repite tu contraseña",
                        valor = viewModel.confirmar,
                        alCambiar = viewModel::cambiarConfirmar,
                        icono = Icons.Filled.VerifiedUser,
                        error = viewModel.errorConfirmar,
                        teclado = KeyboardType.Password,
                        ime = ImeAction.Done,
                        alTerminar = { focus.clearFocus() },
                        esPassword = true,
                        verPassword = verPassword,
                        alAlternarPassword = null,
                        habilitado = !cargando
                    )
                    CasillaTerminos(
                        marcada = viewModel.aceptaTerminos,
                        alCambiar = viewModel::cambiarAceptaTerminos,
                        alAbrirTerminos = onTerminos
                    )
                }
            }

            (viewModel.estado as? AuthEstado.Error)?.let { BannerError(it.mensaje) }

            BotonPrincipal(
                texto = if (registro) "Crear cuenta" else "Iniciar sesión",
                habilitado = viewModel.formularioValido && !cargando,
                cargando = cargando,
                alPulsar = { focus.clearFocus(); viewModel.enviar() }
            )
        }
    }
}

/** Selector "Iniciar sesion / Registrarse" con la pestana activa en verde. */
@Composable
private fun SelectorModo(modo: ModoAuth, alElegir: (ModoAuth) -> Unit) {
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
private fun Pestana(texto: String, activa: Boolean, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
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
private fun Campo(
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
private fun CasillaTerminos(marcada: Boolean, alCambiar: (Boolean) -> Unit, alAbrirTerminos: () -> Unit) {
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
private fun BannerError(mensaje: String) {
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
private fun BotonPrincipal(texto: String, habilitado: Boolean, cargando: Boolean, alPulsar: () -> Unit) {
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

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Login", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewLogin() {
    MobilekotlinTheme(dynamicColor = false) { AuthScreen(modoInicial = "login") }
}

@Preview(name = "Registro", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewRegistro() {
    MobilekotlinTheme(dynamicColor = false) { AuthScreen(modoInicial = "registro") }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) { AuthScreen(modoInicial = "login") }
}
