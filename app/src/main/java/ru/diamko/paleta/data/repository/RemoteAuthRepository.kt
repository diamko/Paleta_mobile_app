package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.network.NetworkError
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.data.remote.api.AuthApi
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.AuthDataDto
import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override suspend fun login(login: String, password: String): User = withContext(Dispatchers.IO) {
        val envelope = authApi.login(
            ru.diamko.paleta.data.remote.dto.LoginRequestDto(
                login = login.trim(),
                password = password,
            ),
        )
        val data = envelope.unwrap("Не удалось выполнить вход")
        tokenStore.saveTokens(data.tokens.access_token, data.tokens.refresh_token)
        data.user.toDomain()
    }

    override suspend fun register(username: String, email: String, password: String): User = withContext(Dispatchers.IO) {
        val envelope = authApi.register(
            ru.diamko.paleta.data.remote.dto.RegisterRequestDto(
                username = username.trim(),
                email = email.trim().lowercase(),
                password = password,
            ),
        )
        val data = envelope.unwrap("Не удалось выполнить регистрацию")
        tokenStore.saveTokens(data.tokens.access_token, data.tokens.refresh_token)
        data.user.toDomain()
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { authApi.logout() }
        tokenStore.clear()
    }

    override suspend fun currentUser(): User? = withContext(Dispatchers.IO) {
        val access = tokenStore.readAccessToken() ?: return@withContext null
        if (access.isBlank()) return@withContext null

        val envelope = authApi.me()
        envelope.unwrap("Не удалось получить профиль").toDomain()
    }

    private fun ApiEnvelope<AuthDataDto>.unwrap(defaultMessage: String): AuthDataDto {
        if (success && data != null) {
            return data
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun ApiEnvelope<ru.diamko.paleta.data.remote.dto.UserDto>.unwrap(defaultMessage: String): ru.diamko.paleta.data.remote.dto.UserDto {
        if (success && data != null) {
            return data
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun ru.diamko.paleta.data.remote.dto.UserDto.toDomain(): User {
        return User(id = id, username = username, email = email)
    }
}
