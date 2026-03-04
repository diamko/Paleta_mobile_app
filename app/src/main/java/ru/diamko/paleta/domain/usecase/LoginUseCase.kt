package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(login: String, password: String): User {
        return authRepository.login(login, password)
    }
}
