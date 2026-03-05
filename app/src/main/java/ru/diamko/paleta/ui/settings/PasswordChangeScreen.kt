package ru.diamko.paleta.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import ru.diamko.paleta.ui.auth.AuthUiState
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaMessageBanner
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.paletaTextFieldColors

@Composable
fun PasswordChangeScreen(
    authState: AuthUiState,
    onSendCode: () -> Unit,
    onChangePassword: (code: String, newPassword: String, confirmPassword: String) -> Unit,
    onBack: () -> Unit,
    onClearMessages: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PaletaCard(modifier = Modifier.fillMaxWidth()) {
                PaletaSectionTitle(
                    title = stringResource(id = R.string.password_change_title),
                    subtitle = stringResource(id = R.string.password_change_subtitle),
                )

                PaletaGhostButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.send_code_email),
                    onClick = onSendCode,
                    enabled = !authState.isLoading,
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.filter(Char::isDigit).take(6)
                        onClearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text(stringResource(id = R.string.code_6_digits)) },
                    colors = paletaTextFieldColors(),
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        onClearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text(stringResource(id = R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = paletaTextFieldColors(),
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        onClearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text(stringResource(id = R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = paletaTextFieldColors(),
                )

                authState.error?.let { PaletaMessageBanner(message = it, isError = true) }
                authState.infoMessage?.let { PaletaMessageBanner(message = it, isError = false) }

                PaletaPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.change_password),
                    onClick = { onChangePassword(code, newPassword, confirmPassword) },
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
    }
}
