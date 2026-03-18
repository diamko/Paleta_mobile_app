/**
 * Модуль: LogoutUseCase.
 * Назначение: Use-case: выход из учётной записи.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
