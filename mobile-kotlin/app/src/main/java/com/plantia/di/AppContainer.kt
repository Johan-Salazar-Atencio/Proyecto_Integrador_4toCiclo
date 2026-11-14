package com.plantia.di

import com.plantia.data.ApiAuthRepository
import com.plantia.data.AuthRepository
import com.plantia.data.CatalogoRepository
import com.plantia.data.CollectionRepository
import com.plantia.data.GardenRepository
import com.plantia.data.HomeRepository
import com.plantia.data.RemoteCatalogoRepository
import com.plantia.data.RemoteCollectionRepository
import com.plantia.data.RemoteGardenRepository
import com.plantia.data.RemoteHomeRepository
import com.plantia.data.RemoteScanRepository
import com.plantia.data.RemoteUsuarioRepository
import com.plantia.data.ScanRepository
import com.plantia.data.UsuarioRepository

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
