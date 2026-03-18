/**
 * Модуль: AuthTokens.
 * Назначение: Доменная модель токенов авторизации (access + refresh).
 */
package ru.diamko.paleta.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
