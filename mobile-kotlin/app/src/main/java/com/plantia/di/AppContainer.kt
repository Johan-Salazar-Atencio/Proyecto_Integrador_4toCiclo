package com.plantia.di

import com.plantia.data.repository.ApiAuthRepository
import com.plantia.data.repository.AuthRepository
import com.plantia.data.repository.CatalogoRepository
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.HomeRepository
import com.plantia.data.repository.RemoteCatalogoRepository
import com.plantia.data.repository.RemoteCollectionRepository
import com.plantia.data.repository.RemoteGardenRepository
import com.plantia.data.repository.RemoteHomeRepository
import com.plantia.data.repository.RemoteScanRepository
import com.plantia.data.repository.RemoteUsuarioRepository
import com.plantia.data.repository.ScanRepository
import com.plantia.data.repository.UsuarioRepository

/**
 * Punto unico donde se crean los repositorios: cada uno existe una sola vez en toda la app.
 * Los ViewModels los reciben por constructor desde sus `Factory`.
 */
object AppContainer {
    val auth: AuthRepository by lazy { ApiAuthRepository() }
    val scan: ScanRepository by lazy { RemoteScanRepository() }
    val collection: CollectionRepository by lazy { RemoteCollectionRepository() }
    val garden: GardenRepository by lazy { RemoteGardenRepository() }
    val home: HomeRepository by lazy { RemoteHomeRepository() }
    val usuario: UsuarioRepository by lazy { RemoteUsuarioRepository() }
    val catalogo: CatalogoRepository by lazy { RemoteCatalogoRepository() }
}
