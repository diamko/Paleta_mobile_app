package ru.diamko.paleta.domain.repository

import ru.diamko.paleta.domain.model.User

interface AuthRepository {
    suspend fun login(login: String, password: String): User
    suspend fun register(username: String, email: String, password: String): User
    suspend fun requestPasswordResetCode(email: String)
    suspend fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String)
    suspend fun updateProfile(username: String, email: String, currentPassword: String): User
    suspend fun sendProfilePasswordCode()
    suspend fun changeProfilePassword(code: String, newPassword: String, confirmPassword: String)
    suspend fun logout()
    suspend fun currentUser(): User?
}
