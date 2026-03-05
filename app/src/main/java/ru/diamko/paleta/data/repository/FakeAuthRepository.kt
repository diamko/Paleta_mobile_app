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

    override suspend fun requestPasswordResetCode(email: String) = withContext(Dispatchers.IO) {
        FakeBackend.requestPasswordResetCode(email.trim().lowercase())
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String,
    ) = withContext(Dispatchers.IO) {
        FakeBackend.resetPassword(
            email = email.trim().lowercase(),
            code = code.trim(),
            newPassword = newPassword,
            confirmPassword = confirmPassword,
        )
    }

    override suspend fun updateProfile(
        username: String,
        email: String,
        currentPassword: String,
    ): User = withContext(Dispatchers.IO) {
        val accessToken = tokenStore.readAccessToken() ?: error("Сессия истекла")
        FakeBackend.updateProfile(
            accessToken = accessToken,
            username = username.trim(),
            email = email.trim().lowercase(),
            currentPassword = currentPassword,
        )
    }

    override suspend fun sendProfilePasswordCode() = withContext(Dispatchers.IO) {
        val accessToken = tokenStore.readAccessToken() ?: error("Сессия истекла")
        FakeBackend.sendProfilePasswordCode(accessToken)
    }

    override suspend fun changeProfilePassword(
        code: String,
        newPassword: String,
        confirmPassword: String,
    ) = withContext(Dispatchers.IO) {
        val accessToken = tokenStore.readAccessToken() ?: error("Сессия истекла")
        FakeBackend.changeProfilePassword(
            accessToken = accessToken,
            code = code.trim(),
            newPassword = newPassword,
            confirmPassword = confirmPassword,
        )
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
