package ru.diamko.paleta.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.domain.model.User
import ru.diamko.paleta.domain.usecase.GetCurrentUserUseCase
import ru.diamko.paleta.domain.usecase.LoginUseCase
import ru.diamko.paleta.domain.usecase.LogoutUseCase
import ru.diamko.paleta.domain.usecase.RegisterUseCase

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun login(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните логин и пароль") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { loginUseCase(login, password) }
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isLoading = false,
                            user = user,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isLoading = false,
                            error = error.message ?: "Ошибка входа",
                        )
                    }
                }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { registerUseCase(username, email, password) }
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isLoading = false,
                            user = user,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isLoading = false,
                            error = error.message ?: "Ошибка регистрации",
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { logoutUseCase() }
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoading = false,
                    user = null,
                    error = null,
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            runCatching { getCurrentUserUseCase() }
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            user = user,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            user = null,
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(
                        loginUseCase = LoginUseCase(container.authRepository),
                        registerUseCase = RegisterUseCase(container.authRepository),
                        logoutUseCase = LogoutUseCase(container.authRepository),
                        getCurrentUserUseCase = GetCurrentUserUseCase(container.authRepository),
                    ) as T
                }
            }
        }
    }
}
