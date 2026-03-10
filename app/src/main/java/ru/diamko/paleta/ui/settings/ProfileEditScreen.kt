package ru.diamko.paleta.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.core.validation.AuthValidation
import ru.diamko.paleta.ui.auth.AuthUiState
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.PaletaTopBannerHost
import ru.diamko.paleta.ui.components.paletaTextFieldColors

@Composable
fun ProfileEditScreen(
    authState: AuthUiState,
    onSave: (username: String, email: String, currentPassword: String) -> Unit,
    onBack: () -> Unit,
    onClearMessages: () -> Unit,
) {
    var username by remember(authState.user?.username) { mutableStateOf(authState.user?.username.orEmpty()) }
    var email by remember(authState.user?.email) { mutableStateOf(authState.user?.email.orEmpty()) }
    var currentPassword by remember { mutableStateOf("") }

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = stringResource(id = R.string.profile_title),
                        subtitle = stringResource(id = R.string.profile_subtitle),
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = AuthValidation.sanitizeUsername(it)
                            onClearMessages()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(id = R.string.username_hint)) },
                        colors = paletaTextFieldColors(),
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it.take(AuthValidation.EMAIL_MAX)
                            onClearMessages()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(id = R.string.email_hint)) },
                        colors = paletaTextFieldColors(),
                    )

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it.take(AuthValidation.PASSWORD_MAX)
                            onClearMessages()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(id = R.string.current_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = paletaTextFieldColors(),
                    )

                    PaletaPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.save_profile),
                        onClick = { onSave(username, email, currentPassword) },
                        enabled = !authState.isLoading,
                        isLoading = authState.isLoading,
                    )

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.back),
                        onClick = onBack,
                    )
                }
            }

            PaletaTopBannerHost(
                error = authState.error,
                info = authState.infoMessage,
            )
        }
    }
}
