/**
 * Модуль: AuthDtos.
 * Назначение: DTO авторизации: запросы и ответы для login, register, reset.
 */
package ru.diamko.paleta.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val login: String,
    val password: String,
    val device_name: String = "Android",
)

@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String,
    val device_name: String = "Android",
)

@Serializable
data class ForgotPasswordRequestDto(
    val email: String,
)

@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    val new_password: String,
    val confirm_password: String,
)

@Serializable
data class RefreshRequestDto(
    val refresh_token: String,
)

@Serializable
data class UpdateProfileRequestDto(
    val username: String,
    val email: String,
    val current_password: String,
)

@Serializable
data class ChangePasswordRequestDto(
    val code: String,
    val new_password: String,
    val confirm_password: String,
)

@Serializable
data class AuthDataDto(
    val user: UserDto,
    val tokens: TokensDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
)

@Serializable
data class TokensDto(
    val access_token: String,
    val refresh_token: String,
)
