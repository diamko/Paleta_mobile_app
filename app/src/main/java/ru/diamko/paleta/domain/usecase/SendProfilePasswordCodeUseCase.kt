/**
 * Модуль: SendProfilePasswordCodeUseCase.
 * Назначение: Use-case: отправка кода подтверждения для смены пароля.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.AuthRepository

class SendProfilePasswordCodeUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.sendProfilePasswordCode()
    }
}
