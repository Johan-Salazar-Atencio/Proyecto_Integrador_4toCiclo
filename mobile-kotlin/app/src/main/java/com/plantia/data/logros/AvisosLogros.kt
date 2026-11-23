package com.plantia.data.logros

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.plantia.MainActivity
import com.plantia.R
import com.plantia.data.model.LogroNuevo

/**
 * Notificaciones locales de logros desbloqueados con la app cerrada, y la memoria de cuales ya se avisaron
 * en este telefono (por cuenta) para no repetir el aviso.
 */
object AvisosLogros {

    private const val PREFS = "plantia_logros_avisos"
    private const val CANAL = "logros"
    private const val CODIGO_NOTIFICACION = 4201
    private const val KEY_PERMISO_PEDIDO = "permiso_pedido"

    private fun clave(userId: Int) = "avisados_$userId"

    /** Ids de logros que ya se notificaron a esa cuenta en este telefono. */
    fun avisados(contexto: Context, userId: Int): Set<Int> =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(clave(userId), emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }.toSet()

    fun marcarAvisados(contexto: Context, userId: Int, ids: Collection<Int>) {
        val prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val todos = avisados(contexto, userId) + ids
        prefs.edit().putStringSet(clave(userId), todos.map { it.toString() }.toSet()).apply()
    }

    /** Los pendientes que todavia no se notificaron, sin repetir ids. */
    fun porAvisar(pendientes: List<LogroNuevo>, avisados: Set<Int>): List<LogroNuevo> =
        pendientes.filter { it.idLogro > 0 && it.idLogro !in avisados }.distinctBy { it.idLogro }

    /** Android 13+ pide el permiso en tiempo de ejecucion; antes de eso siempre esta concedido. */
    fun permisoConcedido(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** true si todavia no se le pidio el permiso al usuario y conviene hacerlo (una sola vez). */
    fun debePedirPermiso(contexto: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 && !permisoConcedido(contexto) &&
            !contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PERMISO_PEDIDO, false)

    fun marcarPermisoPedido(contexto: Context) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PERMISO_PEDIDO, true).apply()
    }

    /** Se puede mostrar una notificacion ahora (permiso concedido y notificaciones no bloqueadas). */
    fun puedeNotificar(contexto: Context): Boolean =
        permisoConcedido(contexto) && NotificationManagerCompat.from(contexto).areNotificationsEnabled()

    /** Una notificacion con el logro (o el resumen si son varios); al tocarla se abre la app. */
    fun mostrar(contexto: Context, logros: List<LogroNuevo>) {
        if (logros.isEmpty()) return
        val gestor = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gestor.createNotificationChannel(
                NotificationChannel(CANAL, "Logros", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Avisos de logros desbloqueados"
                }
            )
        }
        val abrir = PendingIntent.getActivity(
            contexto, 0, Intent(contexto, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (titulo, texto) = textoAviso(logros)
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        runCatching { gestor.notify(CODIGO_NOTIFICACION, aviso) }
    }

    /** Titulo y texto del aviso: un logro con su descripcion, o la lista de titulos si son varios. */
    internal fun textoAviso(logros: List<LogroNuevo>): Pair<String, String> =
        if (logros.size == 1) {
            "¡Logro desbloqueado!" to "${logros[0].titulo}: ${logros[0].descripcion}".trimEnd(':', ' ')
        } else {
            "¡${logros.size} logros desbloqueados!" to logros.joinToString(" · ") { it.titulo }
        }
}
