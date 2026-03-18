/**
 * Модуль: RequestPasswordResetCodeUseCase.
 * Назначение: Use-case: запрос кода восстановления пароля на email.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.AuthRepository

class RequestPasswordResetCodeUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String) {
        authRepository.requestPasswordResetCode(email)
    }
}
