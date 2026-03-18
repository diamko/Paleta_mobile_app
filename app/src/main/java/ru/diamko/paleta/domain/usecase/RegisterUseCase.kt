/**
 * Модуль: RegisterUseCase.
 * Назначение: Use-case: регистрация нового пользователя.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(username: String, email: String, password: String): User {
        return authRepository.register(username, email, password)
    }
}
