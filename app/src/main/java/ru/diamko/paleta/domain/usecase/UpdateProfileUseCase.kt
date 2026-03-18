/**
 * Модуль: UpdateProfileUseCase.
 * Назначение: Use-case: обновление профиля пользователя.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class UpdateProfileUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        username: String,
        email: String,
        currentPassword: String,
    ): User {
        return authRepository.updateProfile(username, email, currentPassword)
    }
}
