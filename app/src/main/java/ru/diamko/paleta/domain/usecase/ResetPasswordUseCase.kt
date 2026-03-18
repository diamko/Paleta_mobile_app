/**
 * Модуль: ResetPasswordUseCase.
 * Назначение: Use-case: сброс пароля по коду подтверждения.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.AuthRepository

class ResetPasswordUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String,
    ) {
        authRepository.resetPassword(email, code, newPassword, confirmPassword)
    }
}
