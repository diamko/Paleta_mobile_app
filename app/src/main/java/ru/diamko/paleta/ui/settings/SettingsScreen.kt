package ru.diamko.paleta.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.ui.auth.AuthUiState
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authState: AuthUiState,
    currentLanguageTag: String,
    isApplyingLanguage: Boolean,
    onChangeLanguage: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPasswordChange: () -> Unit,
    onOpenFaq: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text(stringResource(id = R.string.settings)) },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = stringResource(id = R.string.profile_title),
                        subtitle = stringResource(id = R.string.profile_session_subtitle),
                    )
                    Text(
                        text = authState.user?.username ?: "-",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(text = authState.user?.email ?: "-")

                    PaletaSectionTitle(
                        title = stringResource(id = R.string.language_title),
                        subtitle = stringResource(id = R.string.language_subtitle),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = currentLanguageTag == "ru",
                            onClick = { onChangeLanguage("ru") },
                            enabled = !isApplyingLanguage,
                            label = { Text(stringResource(id = R.string.language_ru)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x1A1976D2),
                            ),
                        )
                        FilterChip(
                            selected = currentLanguageTag == "en",
                            onClick = { onChangeLanguage("en") },
                            enabled = !isApplyingLanguage,
                            label = { Text(stringResource(id = R.string.language_en)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x1A1976D2),
                            ),
                        )
                    }

                    if (isApplyingLanguage) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(top = 2.dp))
                            Text(text = stringResource(id = R.string.language_switching))
                        }
                    }

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.edit_profile),
                        onClick = onOpenProfile,
                        enabled = !isApplyingLanguage,
                    )
                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.password_change_title),
                        onClick = onOpenPasswordChange,
                        enabled = !isApplyingLanguage,
                    )
                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.faq),
                        onClick = onOpenFaq,
                        enabled = !isApplyingLanguage,
                    )
                    PaletaPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.logout),
                        onClick = onLogout,
                        enabled = !isApplyingLanguage,
                    )
                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.cancel),
                        onClick = onBack,
                        enabled = !isApplyingLanguage,
                    )
                }
            }
        }
    }
}
