package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class FakeAuthRepository(
    private val tokenStore: TokenStore,
) : AuthRepository {

    override suspend fun login(login: String, password: String): User = withContext(Dispatchers.IO) {
        val (user, tokens) = FakeBackend.login(login.trim(), password)
        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
        user
    }

    override suspend fun register(username: String, email: String, password: String): User = withContext(Dispatchers.IO) {
        val (user, tokens) = FakeBackend.register(
            username = username.trim(),
            email = email.trim().lowercase(),
            password = password,
        )
        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
        user
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        val access = tokenStore.readAccessToken()
        val refresh = tokenStore.readRefreshToken()
        FakeBackend.revoke(accessToken = access, refreshToken = refresh)
        tokenStore.clear()
    }

    override suspend fun currentUser(): User? = withContext(Dispatchers.IO) {
        val accessToken = tokenStore.readAccessToken() ?: return@withContext null
        FakeBackend.userByAccessToken(accessToken)
    }
}
