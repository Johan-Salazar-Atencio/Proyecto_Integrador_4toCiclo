package com.plantia.data.logros

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.plantia.data.local.ConexionConfig
import com.plantia.data.local.EstadoApp
import com.plantia.data.local.Sesion
import com.plantia.data.repository.LogrosRepository
import com.plantia.data.repository.RemoteLogrosRepository
import java.util.concurrent.TimeUnit

/**
 * Revisa cada cierto tiempo si se desbloqueo algun logro con la app cerrada (por ejemplo al terminar un evento)
 * y avisa con una notificacion local. Solo lee `GET /logros/pendientes`: nunca los marca como vistos, porque eso
 * pasa cuando el usuario ve la celebracion dentro de la app.
 *
 * Sin sesion, sin red o con la app a la vista no hace nada y nunca falla.
 */
class RevisionLogrosWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    /** Se puede cambiar en pruebas. */
    internal var repositorio: LogrosRepository = RemoteLogrosRepository()

    override suspend fun doWork(): Result {
        val contexto = applicationContext
        // El proceso puede haber nacido solo para este trabajo: la sesion y la conexion no estarian cargadas
        Sesion.init(contexto)
        ConexionConfig.init(contexto)

        if (!Sesion.token.value.isNotBlank() || EstadoApp.enPrimerPlano) return Result.success()
        val userId = Sesion.userId.value

        val pendientes = runCatching { repositorio.pendientes() }.getOrNull() ?: return Result.success()
        // Si mientras tanto se cerro la sesion o cambio la cuenta, esos logros ya no son de quien esta
        if (Sesion.userId.value != userId || !Sesion.token.value.isNotBlank()) return Result.success()

        val nuevos = AvisosLogros.porAvisar(pendientes, AvisosLogros.avisados(contexto, userId))
        if (nuevos.isEmpty() || !AvisosLogros.puedeNotificar(contexto)) return Result.success()

        AvisosLogros.mostrar(contexto, nuevos)
        AvisosLogros.marcarAvisados(contexto, userId, nuevos.map { it.idLogro })
        return Result.success()
    }
}

/** Programa la revision periodica de logros (una sola, aunque se llame en cada arranque). */
object ProgramadorLogros {
    private const val NOMBRE = "revision_logros"
    private const val CADA_MINUTOS = 30L

    fun programar(contexto: Context) {
        val peticion = PeriodicWorkRequestBuilder<RevisionLogrosWorker>(CADA_MINUTOS, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // KEEP: si ya esta programada no se reinicia el reloj en cada arranque
        runCatching {
            WorkManager.getInstance(contexto.applicationContext)
                .enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.KEEP, peticion)
        }
    }
}
