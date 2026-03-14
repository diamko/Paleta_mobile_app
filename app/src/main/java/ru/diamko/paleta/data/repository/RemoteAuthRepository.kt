package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.network.NetworkError
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.core.storage.UserStore
import ru.diamko.paleta.data.remote.api.AuthApi
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.AuthDataDto
import ru.diamko.paleta.data.remote.dto.ChangePasswordRequestDto
import ru.diamko.paleta.data.remote.dto.ForgotPasswordRequestDto
import ru.diamko.paleta.data.remote.dto.LoginRequestDto
import ru.diamko.paleta.data.remote.dto.RegisterRequestDto
import ru.diamko.paleta.data.remote.dto.ResetPasswordRequestDto
import ru.diamko.paleta.data.remote.dto.UpdateProfileRequestDto
import ru.diamko.paleta.data.remote.dto.UserDto
import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val userStore: UserStore,
) : AuthRepository {

    override suspend fun login(login: String, password: String): User = withContext(Dispatchers.IO) {
        val envelope = authApi.login(
            LoginRequestDto(
                login = login.trim(),
                password = password,
            ),
        )
        val data = envelope.unwrapAuth("Не удалось выполнить вход")
        tokenStore.saveTokens(data.tokens.access_token, data.tokens.refresh_token)
        val user = data.user.toDomain()
        userStore.saveUser(user)
        user
    }

    override suspend fun register(username: String, email: String, password: String): User = withContext(Dispatchers.IO) {
        val envelope = authApi.register(
            RegisterRequestDto(
                username = username.trim(),
                email = email.trim().lowercase(),
                password = password,
            ),
        )
        val data = envelope.unwrapAuth("Не удалось выполнить регистрацию")
        tokenStore.saveTokens(data.tokens.access_token, data.tokens.refresh_token)
        val user = data.user.toDomain()
        userStore.saveUser(user)
        user
    }

    override suspend fun requestPasswordResetCode(email: String) = withContext(Dispatchers.IO) {
        val envelope = authApi.forgotPassword(
            ForgotPasswordRequestDto(email = email.trim().lowercase()),
        )
        envelope.unwrapUnit("Не удалось отправить код восстановления")
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String,
    ) = withContext(Dispatchers.IO) {
        val envelope = authApi.resetPassword(
            ResetPasswordRequestDto(
                email = email.trim().lowercase(),
                code = code.trim(),
                new_password = newPassword,
                confirm_password = confirmPassword,
            ),
        )
        envelope.unwrapUnit("Не удалось сбросить пароль")
    }

    override suspend fun updateProfile(
        username: String,
        email: String,
        currentPassword: String,
    ): User = withContext(Dispatchers.IO) {
        val envelope = authApi.updateProfile(
            UpdateProfileRequestDto(
                username = username.trim(),
                email = email.trim().lowercase(),
                current_password = currentPassword,
            ),
        )
        val user = envelope.unwrapUser("Не удалось обновить профиль").toDomain()
        userStore.saveUser(user)
        user
    }

    override suspend fun sendProfilePasswordCode() = withContext(Dispatchers.IO) {
        val envelope = authApi.sendProfilePasswordCode()
        envelope.unwrapUnit("Не удалось отправить код подтверждения")
    }

    override suspend fun changeProfilePassword(
        code: String,
        newPassword: String,
        confirmPassword: String,
    ) = withContext(Dispatchers.IO) {
        val envelope = authApi.changeProfilePassword(
            ChangePasswordRequestDto(
                code = code.trim(),
                new_password = newPassword,
                confirm_password = confirmPassword,
            ),
        )
        envelope.unwrapUnit("Не удалось изменить пароль")
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { authApi.logout() }
        tokenStore.clear()
        userStore.clearUser()
    }

    override suspend fun currentUser(): User? = withContext(Dispatchers.IO) {
        val access = tokenStore.readAccessToken() ?: return@withContext null
        if (access.isBlank()) return@withContext null

        runCatching { authApi.me().unwrapUser("Не удалось получить профиль").toDomain() }
            .onSuccess { user ->
                userStore.saveUser(user)
                return@withContext user
            }
            .onFailure {
                // Офлайн или сервер недоступен — вернуть кэшированного пользователя
                val cached = userStore.readUser()
                if (cached != null) return@withContext cached
            }
        return@withContext null
    }

    private fun ApiEnvelope<AuthDataDto>.unwrapAuth(defaultMessage: String): AuthDataDto {
        if (success && data != null) {
            return data
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun ApiEnvelope<UserDto>.unwrapUser(defaultMessage: String): UserDto {
        if (success && data != null) {
            return data
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun ApiEnvelope<Unit>.unwrapUnit(defaultMessage: String) {
        if (success) {
            return
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun UserDto.toDomain(): User {
        return User(id = id, username = username, email = email)
    }
}
