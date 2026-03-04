package ru.diamko.paleta.domain.repository

import ru.diamko.paleta.domain.model.User

interface AuthRepository {
    suspend fun login(login: String, password: String): User
    suspend fun register(username: String, email: String, password: String): User
    suspend fun logout()
    suspend fun currentUser(): User?
}
