package com.plantia.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Descubre el backend de Plantia en la red local mediante mDNS.
 *
 * El PC publica el servicio `_plantia._tcp.` (ver `scripts/anunciar-plantia.py`).
 * Asi la app encuentra el equipo sin conocer su IP: da igual a que WiFi se
 * conecte, siempre que el movil y el PC esten en la misma red.
 */
class DescubridorMdns(private val context: Context) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var listener: NsdManager.DiscoveryListener? = null

    // Android solo permite una resolucion a la vez: si ya hay una en curso se
    // espera a que termine.
    private var resolviendo = false

    /** Busca el backend. `onEncontrado` recibe la URL base (http://ip:puerto/). */
    fun descubrir(onEncontrado: (String) -> Unit) {
        if (listener != null) return

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.w(TAG, "No se pudo iniciar el descubrimiento mDNS: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.w(TAG, "Error al detener el descubrimiento mDNS: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(TAG, "Buscando el backend de Plantia en la red local...")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(TAG, "Descubrimiento mDNS detenido")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                val nombre = serviceInfo?.serviceName ?: return
                if (!nombre.contains("plantia", ignoreCase = true)) return
                resolver(serviceInfo, onEncontrado)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.i(TAG, "Servicio perdido: ${serviceInfo?.serviceName}")
            }
        }

        listener = discoveryListener
        try {
            nsdManager.discoverServices(
                TIPO_SERVICIO,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al descubrir servicios mDNS: ${e.message}", e)
            listener = null
        }
    }

    private fun resolver(
        serviceInfo: NsdServiceInfo,
        onEncontrado: (String) -> Unit
    ) {
        if (resolviendo) return
        resolviendo = true
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "No se pudo resolver el servicio: $errorCode")
                resolviendo = false
            }

            override fun onServiceResolved(info: NsdServiceInfo?) {
                resolviendo = false
                val host = info?.host?.hostAddress ?: return
                val url = "http://$host:${info.port}/"
                Log.i(TAG, "Backend encontrado por mDNS: $url")
                onEncontrado(url)
            }
        })
    }

    fun parar() {
        listener?.let { nsdManager.stopServiceDiscovery(it) }
        listener = null
        resolviendo = false
    }

    companion object {
        private const val TAG = "DescubridorMdns"
        private const val TIPO_SERVICIO = "_plantia._tcp."
    }
}
