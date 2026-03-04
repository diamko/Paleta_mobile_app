package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): User? {
        return authRepository.currentUser()
    }
}
