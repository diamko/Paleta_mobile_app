package ru.diamko.paleta.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.core.validation.AuthValidation
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.PaletaTopBannerHost
import ru.diamko.paleta.ui.components.paletaTextFieldColors

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLoginClick: (login: String, password: String) -> Unit,
    onGoRegisterClick: () -> Unit,
    onGoForgotPasswordClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit,
    onClearError: () -> Unit,
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.paleta_logo),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                ),
                            ),
                        ),
                    )
                }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = stringResource(id = R.string.login_title),
                        subtitle = stringResource(id = R.string.login_subtitle),
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = login,
                        onValueChange = {
                            login = it.take(AuthValidation.EMAIL_MAX)
                            onClearError()
                        },
                        label = { Text(stringResource(id = R.string.login_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = paletaTextFieldColors(),
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = {
                            password = it.take(AuthValidation.PASSWORD_MAX)
                            onClearError()
                        },
                        label = { Text(stringResource(id = R.string.password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = paletaTextFieldColors(),
                    )

                    PaletaPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.login_button),
                        onClick = { onLoginClick(login, password) },
                        enabled = !state.isLoading,
                        isLoading = state.isLoading,
                    )

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.go_register),
                        onClick = onGoRegisterClick,
                        enabled = !state.isLoading,
                    )

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.forgot_password_link),
                        onClick = onGoForgotPasswordClick,
                        enabled = !state.isLoading,
                    )

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.continue_as_guest),
                        onClick = onContinueAsGuestClick,
                        enabled = !state.isLoading,
                    )
                }

                Text(
                    text = stringResource(id = R.string.wifi_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PaletaTopBannerHost(
                error = state.error,
                info = state.infoMessage,
            )
        }
    }
}
