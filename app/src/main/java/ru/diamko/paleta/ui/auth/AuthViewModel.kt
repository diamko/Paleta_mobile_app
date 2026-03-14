package ru.diamko.paleta.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.diamko.paleta.R
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.core.validation.AuthValidation
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

@androidx.compose.runtime.Immutable
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
    private val getString: (Int) -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    private fun resolveErrorMessage(error: Throwable, fallbackResId: Int): String {
        return when (error) {
            is IllegalArgumentException, is IllegalStateException -> error.message ?: getString(fallbackResId)
            else -> getString(fallbackResId)
        }
    }

    fun login(login: String, password: String) {
        val loginTrimmed = login.trim()
        if (loginTrimmed.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = getString(R.string.auth_fill_login_password)) }
            return
        }
        val loginError = if (loginTrimmed.contains('@')) {
            if (!AuthValidation.isValidEmail(loginTrimmed)) getString(R.string.auth_invalid_email) else null
        } else {
            if (!AuthValidation.isValidUsername(loginTrimmed)) getString(R.string.auth_invalid_username) else null
        }
        if (loginError != null) {
            _uiState.update { it.copy(error = loginError) }
            return
        }
        if (!AuthValidation.isValidPassword(password)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_password)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { loginUseCase(loginTrimmed, password) }
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
                            error = resolveErrorMessage(error, R.string.auth_error_login),
                        )
                    }
                }
        }
    }

    fun register(username: String, email: String, password: String) {
        val usernameTrimmed = username.trim()
        val emailTrimmed = email.trim()
        if (usernameTrimmed.isBlank() || emailTrimmed.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = getString(R.string.auth_fill_all_fields)) }
            return
        }
        if (!AuthValidation.isValidUsername(usernameTrimmed)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_username)) }
            return
        }
        if (!AuthValidation.isValidEmail(emailTrimmed)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_email)) }
            return
        }
        if (!AuthValidation.isValidPassword(password)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_password)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { registerUseCase(usernameTrimmed, emailTrimmed, password) }
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
                            error = resolveErrorMessage(error, R.string.auth_error_register),
                        )
                    }
                }
        }
    }

    fun requestPasswordResetCode(email: String, onDone: () -> Unit = {}) {
        val emailTrimmed = email.trim()
        if (emailTrimmed.isBlank()) {
            _uiState.update { it.copy(error = getString(R.string.auth_enter_email)) }
            return
        }
        if (!AuthValidation.isValidEmail(emailTrimmed)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_email)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { requestPasswordResetCodeUseCase(emailTrimmed) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            infoMessage = getString(R.string.auth_reset_code_sent_if_exists),
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = resolveErrorMessage(error, R.string.auth_error_send_code),
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
        val emailTrimmed = email.trim()
        if (emailTrimmed.isBlank() || code.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(error = getString(R.string.auth_fill_all_fields)) }
            return
        }
        if (!AuthValidation.isValidEmail(emailTrimmed)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_email)) }
            return
        }
        if (!AuthValidation.isValidPassword(newPassword)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_password)) }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(error = getString(R.string.auth_passwords_not_match)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { resetPasswordUseCase(emailTrimmed, code, newPassword, confirmPassword) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            infoMessage = getString(R.string.auth_password_updated_login_again),
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = resolveErrorMessage(error, R.string.auth_error_update_password),
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
        val usernameTrimmed = username.trim()
        val emailTrimmed = email.trim()
        if (usernameTrimmed.isBlank() || emailTrimmed.isBlank() || currentPassword.isBlank()) {
            _uiState.update { it.copy(error = getString(R.string.auth_fill_all_fields)) }
            return
        }
        if (!AuthValidation.isValidUsername(usernameTrimmed)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_username)) }
            return
        }
        if (!AuthValidation.isValidEmail(emailTrimmed)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_email)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            runCatching { updateProfileUseCase(usernameTrimmed, emailTrimmed, currentPassword) }
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            error = null,
                            infoMessage = getString(R.string.auth_profile_updated),
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = resolveErrorMessage(error, R.string.auth_error_update_profile),
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
                            infoMessage = getString(R.string.auth_profile_password_code_sent),
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = resolveErrorMessage(error, R.string.auth_error_send_code),
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
            _uiState.update { it.copy(error = getString(R.string.auth_fill_all_fields)) }
            return
        }
        if (!AuthValidation.isValidPassword(newPassword)) {
            _uiState.update { it.copy(error = getString(R.string.auth_invalid_password)) }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(error = getString(R.string.auth_passwords_not_match)) }
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
                            infoMessage = getString(R.string.auth_password_changed),
                        )
                    }
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = resolveErrorMessage(error, R.string.auth_error_change_password),
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
                        getString = container::getString,
                    ) as T
                }
            }
        }
    }
}
