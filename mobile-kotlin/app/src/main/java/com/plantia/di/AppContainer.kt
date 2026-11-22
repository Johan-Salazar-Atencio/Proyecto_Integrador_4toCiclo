package com.plantia.di

import com.plantia.data.local.Sesion
import com.plantia.data.logros.GestorLogros
import com.plantia.data.logros.SesionActual
import com.plantia.data.repository.ApiAuthRepository
import com.plantia.data.repository.ArteRepository
import com.plantia.data.repository.AuthRepository
import com.plantia.data.repository.CatalogoRepository
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.HomeRepository
import com.plantia.data.repository.LogrosRepository
import com.plantia.data.repository.RemoteLogrosRepository
import com.plantia.data.repository.RemoteArteRepository
import com.plantia.data.repository.RemoteCatalogoRepository
import com.plantia.data.repository.RemoteCollectionRepository
import com.plantia.data.repository.RemoteGardenRepository
import com.plantia.data.repository.RemoteHomeRepository
import com.plantia.data.repository.RemoteScanRepository
import com.plantia.data.repository.RemoteTiendaRepository
import com.plantia.data.repository.RemoteUsuarioRepository
import com.plantia.data.repository.ScanRepository
import com.plantia.data.repository.TiendaRepository
import com.plantia.data.repository.UsuarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine

/**
 * Punto unico donde se crean los repositorios: cada uno existe una sola vez en toda la app.
 * Los ViewModels los reciben por constructor desde sus `Factory`.
 */
object AppContainer {
    val auth: AuthRepository by lazy { ApiAuthRepository() }
    val logros: LogrosRepository by lazy { RemoteLogrosRepository() }

    /** Cola de logros por celebrar: se vacia sola al cerrar sesion o cambiar de cuenta. */
    val celebracionLogros: GestorLogros by lazy {
        GestorLogros(
            repo = logros,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            sesion = combine(Sesion.userId, Sesion.token) { id, token -> SesionActual(id, token) }
        )
    }

    val scan: ScanRepository by lazy { RemoteScanRepository(alDesbloquear = { celebracionLogros.encolar(it) }) }
    val collection: CollectionRepository by lazy { RemoteCollectionRepository() }
    val garden: GardenRepository by lazy { RemoteGardenRepository(alDesbloquear = { celebracionLogros.encolar(it) }) }
    val home: HomeRepository by lazy { RemoteHomeRepository() }
    val usuario: UsuarioRepository by lazy { RemoteUsuarioRepository() }
    val arte: ArteRepository by lazy { RemoteArteRepository() }
    val tienda: TiendaRepository by lazy { RemoteTiendaRepository(
            arte = { arte },
            alDesbloquear = { celebracionLogros.encolar(it) },
            alComprar = { celebracionLogros.revisar() }
        ) }
    val catalogo: CatalogoRepository by lazy { RemoteCatalogoRepository() }
}
