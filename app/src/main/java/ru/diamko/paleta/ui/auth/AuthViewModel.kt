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
import ru.diamko.paleta.domain.usecase.ChangeProfilePasswordUseCase
import ru.diamko.paleta.domain.usecase.GetCurrentUserUseCase
import ru.diamko.paleta.domain.usecase.LoginUseCase
import ru.diamko.paleta.domain.usecase.LogoutUseCase
import ru.diamko.paleta.domain.usecase.RegisterUseCase
import ru.diamko.paleta.domain.usecase.RequestPasswordResetCodeUseCase
import ru.diamko.paleta.domain.usecase.ResetPasswordUseCase
import ru.diamko.paleta.domain.usecase.SendProfilePasswordCodeUseCase
import ru.diamko.paleta.domain.usecase.UpdateProfileUseCase

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val infoMessage: String? = null,
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val requestPasswordResetCodeUseCase: RequestPasswordResetCodeUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val sendProfilePasswordCodeUseCase: SendProfilePasswordCodeUseCase,
    private val changeProfilePasswordUseCase: ChangeProfilePasswordUseCase,
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
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
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
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
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

    fun requestPasswordResetCode(email: String, onDone: () -> Unit = {}) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Введите email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { requestPasswordResetCodeUseCase(email) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            infoMessage = "Если email найден, код отправлен",
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось отправить код",
                        )
                    }
                }
        }
    }

    fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String,
        onDone: () -> Unit = {},
    ) {
        if (email.isBlank() || code.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { resetPasswordUseCase(email, code, newPassword, confirmPassword) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            infoMessage = "Пароль обновлён. Войдите с новым паролем",
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось обновить пароль",
                        )
                    }
                }
        }
    }

    fun updateProfile(
        username: String,
        email: String,
        currentPassword: String,
        onDone: () -> Unit = {},
    ) {
        if (username.isBlank() || email.isBlank() || currentPassword.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { updateProfileUseCase(username, email, currentPassword) }
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            error = null,
                            infoMessage = "Профиль обновлён",
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось обновить профиль",
                        )
                    }
                }
        }
    }

    fun sendProfilePasswordCode(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { sendProfilePasswordCodeUseCase() }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            infoMessage = "Код подтверждения отправлен",
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось отправить код",
                        )
                    }
                }
        }
    }

    fun changeProfilePassword(
        code: String,
        newPassword: String,
        confirmPassword: String,
        onDone: () -> Unit = {},
    ) {
        if (code.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { changeProfilePasswordUseCase(code, newPassword, confirmPassword) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            infoMessage = "Пароль успешно изменён",
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось изменить пароль",
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
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

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
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
                        requestPasswordResetCodeUseCase = RequestPasswordResetCodeUseCase(container.authRepository),
                        resetPasswordUseCase = ResetPasswordUseCase(container.authRepository),
                        updateProfileUseCase = UpdateProfileUseCase(container.authRepository),
                        sendProfilePasswordCodeUseCase = SendProfilePasswordCodeUseCase(container.authRepository),
                        changeProfilePasswordUseCase = ChangeProfilePasswordUseCase(container.authRepository),
                        logoutUseCase = LogoutUseCase(container.authRepository),
                        getCurrentUserUseCase = GetCurrentUserUseCase(container.authRepository),
                    ) as T
                }
            }
        }
    }
}
