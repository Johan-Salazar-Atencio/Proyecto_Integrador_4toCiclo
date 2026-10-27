package com.plantia.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.AuthRepository
import com.plantia.data.AuthResultado
import com.plantia.data.MockAuthRepository
import kotlinx.coroutines.launch

enum class ModoAuth { LOGIN, REGISTRO }

sealed interface AuthEstado {
    data object Reposo : AuthEstado
    data object Cargando : AuthEstado
    data class Error(val mensaje: String) : AuthEstado
    data object Exito : AuthEstado
}

/**
 * Estado del formulario de acceso. Sobrevive al ir a "Terminos" y volver, asi no se pierde
 * lo escrito.
 */
class AuthViewModel : ViewModel() {

    // TODO(backend): cambiar por la implementacion real de AuthRepository cuando exista.
    private val repositorio: AuthRepository = MockAuthRepository

    var modo by mutableStateOf(ModoAuth.LOGIN)
        private set
    var nombre by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var confirmar by mutableStateOf("")
        private set
    var aceptaTerminos by mutableStateOf(false)
        private set
    var estado: AuthEstado by mutableStateOf(AuthEstado.Reposo)
        private set

    private var modoInicialAplicado = false

    /** Solo la primera vez: respeta la pestana con la que se abrio la pantalla. */
    fun aplicarModoInicial(inicial: ModoAuth) {
        if (modoInicialAplicado) return
        modoInicialAplicado = true
        modo = inicial
    }

    fun elegirModo(nuevo: ModoAuth) {
        if (nuevo == modo || estado is AuthEstado.Cargando) return
        modo = nuevo
        estado = AuthEstado.Reposo
    }

    fun cambiarNombre(valor: String) { nombre = valor; limpiarError() }
    fun cambiarEmail(valor: String) { email = valor; limpiarError() }
    fun cambiarPassword(valor: String) { password = valor; limpiarError() }
    fun cambiarConfirmar(valor: String) { confirmar = valor; limpiarError() }
    fun cambiarAceptaTerminos(valor: Boolean) { aceptaTerminos = valor }

    private fun limpiarError() {
        if (estado is AuthEstado.Error) estado = AuthEstado.Reposo
    }

    // ---- Errores por campo: solo se muestran cuando el usuario ya escribio algo ----

    val errorNombre: String?
        get() = if (nombre.isNotEmpty() && !esNombreValido(nombre)) "Escribe tu nombre de jardinero" else null

    val errorEmail: String?
        get() = if (email.isNotEmpty() && !esCorreoValido(email)) "Escribe un correo válido" else null

    val errorPassword: String?
        get() = if (modo == ModoAuth.REGISTRO && password.isNotEmpty() && !esPasswordValida(password)) {
            "Mínimo $LARGO_MINIMO_PASSWORD caracteres"
        } else null

    val errorConfirmar: String?
        get() = if (confirmar.isNotEmpty() && !coincidenPasswords(password, confirmar)) {
            "Las contraseñas no coinciden"
        } else null

    /** El boton principal solo se habilita con el formulario completo y correcto. */
    val formularioValido: Boolean
        get() = when (modo) {
            ModoAuth.LOGIN -> esCorreoValido(email) && password.isNotEmpty()
            ModoAuth.REGISTRO ->
                esNombreValido(nombre) && esCorreoValido(email) && esPasswordValida(password) &&
                    coincidenPasswords(password, confirmar) && aceptaTerminos
        }

    fun enviar() {
        if (!formularioValido || estado is AuthEstado.Cargando) return
        estado = AuthEstado.Cargando
        viewModelScope.launch {
            val resultado = when (modo) {
                ModoAuth.LOGIN -> repositorio.iniciarSesion(email, password)
                ModoAuth.REGISTRO -> repositorio.registrar(nombre, email, password)
            }
            estado = when (resultado) {
                is AuthResultado.Ok -> AuthEstado.Exito
                is AuthResultado.Error -> AuthEstado.Error(resultado.mensaje)
            }
        }
    }

    /** Solo para pruebas (cuenta demo del repositorio simulado). */
    fun usarCuentaDemo() {
        email = MockAuthRepository.CORREO_DEMO
        password = MockAuthRepository.CLAVE_DEMO
        estado = AuthEstado.Reposo
    }

    /** La pantalla ya navego: se limpia el estado de exito. */
    fun exitoConsumido() {
        estado = AuthEstado.Reposo
        password = ""
        confirmar = ""
    }
}
