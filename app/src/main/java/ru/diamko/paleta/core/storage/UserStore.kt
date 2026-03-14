package ru.diamko.paleta.core.storage

import ru.diamko.paleta.domain.model.User

interface UserStore {
    suspend fun saveUser(user: User)
    suspend fun readUser(): User?
    suspend fun clearUser()
}
