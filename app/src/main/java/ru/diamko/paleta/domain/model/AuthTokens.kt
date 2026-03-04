package ru.diamko.paleta.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
