package ru.diamko.paleta.core.storage

interface TokenStore {
    suspend fun readAccessToken(): String?
    suspend fun readRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clear()
}
