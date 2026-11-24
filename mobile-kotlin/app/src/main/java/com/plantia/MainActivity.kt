package com.plantia

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.plantia.data.local.ConexionConfig
import com.plantia.data.local.ModoTema
import com.plantia.data.local.PermisoRedLocal
import com.plantia.data.local.Sesion
import com.plantia.data.local.TemaConfig
import com.plantia.data.logros.ProgramadorLogros
import com.plantia.data.network.DescubridorMdns
import com.plantia.data.remote.RetrofitClient
import com.plantia.ui.components.ExplicacionPermiso
import com.plantia.ui.components.TextosPermisos
import com.plantia.ui.navigation.AppNavigation
import com.plantia.ui.theme.MobilekotlinTheme

class MainActivity : ComponentActivity() {

    private val descubridor by lazy { DescubridorMdns(applicationContext) }

    // Android 17 pide permiso para hablar con equipos de la red local (el PC con el backend).
    // Al concederlo se vuelve a buscar el mejor host para no quedarse con un intento fallido.
    private val pedirRedLocal = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        PermisoRedLocal.actualizar(Build.VERSION.SDK_INT, concedido) // QA-62: aviso si se niega
        if (concedido) {
            RetrofitClient.reconectar()
            iniciarBusquedaEnRed()
        }
    }

    private var buscandoEnRed = false

    private fun redLocalConcedida(): Boolean =
        Build.VERSION.SDK_INT < PermisoRedLocal.SDK_CON_PERMISO ||
            ContextCompat.checkSelfPermission(this, PermisoRedLocal.PERMISO) == PackageManager.PERMISSION_GRANTED

    /** Al volver (por ejemplo, de Ajustes) se mira otra vez el permiso de red local y se reconecta si ya se dio. */
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= PermisoRedLocal.SDK_CON_PERMISO) {
            val estabaNegado = PermisoRedLocal.negado.value
            val concedido = redLocalConcedida()
            if (estabaNegado) {
                PermisoRedLocal.actualizar(Build.VERSION.SDK_INT, concedido)
                if (concedido) RetrofitClient.reconectar()
            }
        }
        iniciarBusquedaEnRed()
    }

    /**
     * QA-59: la primera vez no se lanza el dialogo del sistema en frio: antes se muestra una tarjeta que explica para
     * que sirve ([ExplicacionPermiso]) y "Continuar" lo pide.
     */
    private fun revisarPermisoRedLocal() {
        val paso = PermisoRedLocal.pasoAlIniciar(
            Build.VERSION.SDK_INT, redLocalConcedida(), PermisoRedLocal.yaExplicado(this)
        )
        when (paso) {
            PermisoRedLocal.PasoAlIniciar.EXPLICAR -> PermisoRedLocal.mostrarExplicacion(true)
            PermisoRedLocal.PasoAlIniciar.PEDIR -> pedirRedLocal.launch(PermisoRedLocal.PERMISO)
            PermisoRedLocal.PasoAlIniciar.NINGUNO -> Unit
        }
    }

    private fun continuarExplicacionRedLocal() {
        PermisoRedLocal.marcarExplicado(this)
        PermisoRedLocal.mostrarExplicacion(false)
        pedirRedLocal.launch(PermisoRedLocal.PERMISO)
    }

    /**
     * Busca el PC en la red local por mDNS: asi no hay que hardcodear la IP y funciona aunque se cambie de WiFi.
     * Solo con el permiso de red local (QA-59): antes arrancaba al abrir y Android pedia "dispositivos cercanos"
     * sin ninguna explicacion.
     */
    private fun iniciarBusquedaEnRed() {
        if (buscandoEnRed || !PermisoRedLocal.puedeBuscarEnRed(Build.VERSION.SDK_INT, redLocalConcedida())) return
        buscandoEnRed = true
        descubridor.descubrir { url ->
            runOnUiThread { RetrofitClient.actualizaHostMdns(url) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La sesion (id de usuario) y la configuracion de conexion deben estar
        // listas antes de la primera peticion.
        Sesion.init(applicationContext)
        ConexionConfig.init(applicationContext)
        TemaConfig.init(applicationContext)
        // Al girar la pantalla no se vuelve a pedir: el estado de la explicacion sigue en PermisoRedLocal
        if (savedInstanceState == null) revisarPermisoRedLocal()
        // Avisa con una notificacion de los logros que se desbloqueen con la app cerrada
        ProgramadorLogros.programar(applicationContext)

        setContent {
            val modo by TemaConfig.modo.collectAsState()
            val oscuro = when (modo) {
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
                ModoTema.SISTEMA -> isSystemInDarkTheme()
            }
            // La barra de estado sigue el tema elegido, no solo el del telefono.
            val vistaRaiz = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, vistaRaiz).apply {
                    isAppearanceLightStatusBars = !oscuro
                    isAppearanceLightNavigationBars = !oscuro
                }
            }
            MobilekotlinTheme(darkTheme = oscuro) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // AppNavigation monta tambien la celebracion de logros (necesita la navegacion para "Ver mis logros")
                    AppNavigation()
                    val explicarRedLocal by PermisoRedLocal.explicar.collectAsState()
                    if (explicarRedLocal) {
                        ExplicacionPermiso(
                            titulo = TextosPermisos.RED_TITULO,
                            texto = TextosPermisos.RED_EXPLICACION,
                            icono = Icons.Filled.Wifi,
                            onContinuar = ::continuarExplicacionRedLocal
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (buscandoEnRed) descubridor.parar()
    }
}
