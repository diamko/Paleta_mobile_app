package ru.diamko.paleta.ui.auth

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
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaMessageBanner
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.paletaTextFieldColors

@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onRequestCode: (email: String) -> Unit,
    onGoReset: (email: String) -> Unit,
    onBack: () -> Unit,
    onClearMessages: () -> Unit,
) {
    var email by remember { mutableStateOf("") }

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
                    title = stringResource(id = R.string.forgot_password_title),
                    subtitle = stringResource(id = R.string.forgot_password_subtitle),
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        onClearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text(stringResource(id = R.string.email_hint)) },
                    colors = paletaTextFieldColors(),
                )

                state.error?.let { PaletaMessageBanner(message = it, isError = true) }
                state.infoMessage?.let { PaletaMessageBanner(message = it, isError = false) }

                PaletaPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.send_code),
                    onClick = { onRequestCode(email) },
                    enabled = !state.isLoading,
                    isLoading = state.isLoading,
                )

                PaletaGhostButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.have_code),
                    onClick = { onGoReset(email) },
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
