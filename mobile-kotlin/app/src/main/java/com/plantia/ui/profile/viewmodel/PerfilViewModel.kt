package com.plantia.ui.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.local.ConexionConfig
import com.plantia.data.local.ModoTema
import com.plantia.data.local.Sesion
import com.plantia.data.local.TemaConfig
import com.plantia.data.model.UserSummary
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantasResultado
import com.plantia.data.repository.ScanRepository
import com.plantia.data.repository.UsuarioRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Perfil del usuario (GET /api/v1/usuarios/me). */
class PerfilViewModel(
    private val usuarios: UsuarioRepository,
    private val collection: CollectionRepository,
    private val garden: GardenRepository,
    private val scan: ScanRepository
) : ViewModel() {

    var usuario: UserSummary? by mutableStateOf(null)
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    /** Plantas escaneadas en la coleccion (null si aun no se sabe o no se pudo leer). */
    var escaneadas: Int? by mutableStateOf(null)
        private set

    /** Servidor al que esta conectada la app ahora mismo. */
    val baseUrl: StateFlow<String?> = RetrofitClient.baseUrl

    /** Servidor escrito a mano en Ajustes (null si se usa el automatico). */
    val hostManual: StateFlow<String?> = ConexionConfig.host

    val modoTema: StateFlow<ModoTema> = TemaConfig.modo

    /** Plantas digitales del jardin (null si aun no se sabe o no se pudo leer). */
    var digitales: Int? by mutableStateOf(null)
        private set

    init {
        cargar()
    }

    fun recargar() = cargar()

    fun cambiarTema(modo: ModoTema) = TemaConfig.setModo(modo)

    /** Guarda el servidor, vuelve a elegir el host y recarga el perfil con esa conexion. */
    fun guardarHost(host: String?) {
        ConexionConfig.setHost(host)
        RetrofitClient.reconectar()
        cargar()
    }

    /** Deja la sesion como al inicio y descarta el ultimo escaneo en memoria. */
    fun cerrarSesion() {
        Sesion.setUserId(Sesion.USUARIO_POR_DEFECTO)
        Sesion.setToken(null)
        scan.limpiarUltimo()
    }

    private fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            runCatching {
                usuarios.perfil()
            }.onSuccess { perfil ->
                if (perfil == null) {
                    // El backend devuelve 404 si el id no existe: se limpia el
                    // usuario para que se vea el error en vez de un dato viejo.
                    usuario = null
                    error = "No existe un usuario con ese id"
                } else {
                    usuario = perfil
                }
            }.onFailure {
                error = "No se pudo cargar el perfil"
            }
            escaneadas = (collection.listar() as? ColeccionResultado.Con)?.plantas?.size
            digitales = (garden.listar() as? PlantasResultado.Con)?.plantas?.size
            cargando = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PerfilViewModel(
                    AppContainer.usuario,
                    AppContainer.collection,
                    AppContainer.garden,
                    AppContainer.scan
                )
            }
        }
    }
}
