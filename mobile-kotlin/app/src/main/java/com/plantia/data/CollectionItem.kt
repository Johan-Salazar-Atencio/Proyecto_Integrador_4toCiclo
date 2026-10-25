package com.plantia.data

/** Elemento de la coleccion; bloqueado/misterio se muestra como silueta "???". */
data class CollectionItem(
    val id: String,
    val name: String,
    val level: Int,
    val isLocked: Boolean,
    val imageUrl: String? = null
)
