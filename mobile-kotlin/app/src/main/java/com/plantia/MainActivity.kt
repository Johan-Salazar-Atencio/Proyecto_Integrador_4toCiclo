package com.plantia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.plantia.data.ConexionConfig
import com.plantia.data.DescubridorMdns
import com.plantia.data.Sesion
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

        // Busca el PC en la red local por mDNS: asi no hay que hardcodear la IP
        // y funciona aunque se cambie de WiFi.
        descubridor.descubrir { url ->
            runOnUiThread { RetrofitClient.actualizaHostMdns(url) }
        }

        setContent {
            MobilekotlinTheme {
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
