package com.plantia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.plantia.data.ConexionConfig
import com.plantia.data.DescubridorMdns
import com.plantia.data.ModoTema
import com.plantia.data.Sesion
import com.plantia.data.TemaConfig
import com.plantia.data.remote.RetrofitClient
import com.plantia.navigation.AppNavigation
import com.plantia.ui.theme.MobilekotlinTheme

class MainActivity : ComponentActivity() {

    private val descubridor by lazy { DescubridorMdns(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La sesion (id de usuario) y la configuracion de conexion deben estar
        // listas antes de la primera peticion.
        Sesion.init(applicationContext)
        ConexionConfig.init(applicationContext)
        TemaConfig.init(applicationContext)

        // Busca el PC en la red local por mDNS: asi no hay que hardcodear la IP
        // y funciona aunque se cambie de WiFi.
        descubridor.descubrir { url ->
            runOnUiThread { RetrofitClient.actualizaHostMdns(url) }
        }

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
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        descubridor.parar()
    }
}
