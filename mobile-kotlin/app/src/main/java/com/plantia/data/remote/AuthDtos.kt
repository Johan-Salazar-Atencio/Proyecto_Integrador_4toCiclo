package com.plantia.data.remote

data class AuthRegistroRequestDto(
    val nombre: String,
    val email: String,
    val password: String
)

data class AuthLoginRequestDto(
    val email: String,
    val password: String
)

data class AuthUsuarioResponseDto(
    val id: Int,
    val nombre: String,
    val email: String
)
