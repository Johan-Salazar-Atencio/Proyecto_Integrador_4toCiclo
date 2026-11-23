package com.plantia.data.local

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.plantia.MainActivity
import com.plantia.R
import java.util.Calendar

/**
 * Recordatorio diario para cuidar las plantas. Se activa o desactiva desde el Perfil: guarda la eleccion y
 * programa (o cancela) una alarma que repite cada dia a las [HORA] horas con una notificacion local.
 */
object Recordatorios {

    private const val PREFS = "plantia_recordatorios"
    private const val KEY_ACTIVOS = "activos"
    private const val CANAL = "recordatorios_cuidado"
    private const val CODIGO_ALARMA = 4101
    private const val CODIGO_NOTIFICACION = 4102
    const val HORA = 9

    fun activos(contexto: Context): Boolean =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVOS, false)

    fun activar(contexto: Context) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVOS, true).apply()
        programar(contexto)
    }

    fun desactivar(contexto: Context) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVOS, false).apply()
        val alarmas = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmas.cancel(intencionAlarma(contexto))
    }

    /** Vuelve a programar la alarma (por ejemplo, tras reiniciar el telefono). */
    fun programar(contexto: Context) {
        val alarmas = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmas.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            proximaHora(System.currentTimeMillis()),
            AlarmManager.INTERVAL_DAY,
            intencionAlarma(contexto)
        )
    }

    /** Proxima vez que son las [HORA] en punto: hoy si todavia no pasaron, mañana si ya pasaron. */
    internal fun proximaHora(ahora: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ahora
            set(Calendar.HOUR_OF_DAY, HORA)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= ahora) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun intencionAlarma(contexto: Context): PendingIntent = PendingIntent.getBroadcast(
        contexto,
        CODIGO_ALARMA,
        Intent(contexto, ReceptorRecordatorio::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun mostrar(contexto: Context) {
        val gestor = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gestor.createNotificationChannel(
                NotificationChannel(CANAL, "Recordatorios de cuidado", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val abrir = PendingIntent.getActivity(
            contexto, 0, Intent(contexto, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Tus plantas te esperan")
            .setContentText("Riégalas, dales luz y completa tu misión del día.")
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        runCatching { gestor.notify(CODIGO_NOTIFICACION, aviso) }
    }
}

/** Recibe la alarma diaria (muestra el recordatorio) y el arranque del telefono (vuelve a programarla). */
class ReceptorRecordatorio : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (!Recordatorios.activos(contexto)) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Recordatorios.programar(contexto)
        else Recordatorios.mostrar(contexto)
    }
}
