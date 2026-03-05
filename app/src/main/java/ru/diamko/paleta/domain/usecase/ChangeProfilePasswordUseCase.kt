package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.AuthRepository

class ChangeProfilePasswordUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        code: String,
        newPassword: String,
        confirmPassword: String,
    ) {
        authRepository.changeProfilePassword(code, newPassword, confirmPassword)
    }
}
