package ru.diamko.paleta.ui.palettes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteEditorScreen(
    paletteId: Long?,
    paletteViewModel: PaletteViewModel,
    onBack: () -> Unit,
) {
    val existing = paletteId?.let { paletteViewModel.paletteById(it) }

    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var colorsInput by remember(existing?.id) { mutableStateOf(existing?.colors?.joinToString(",").orEmpty()) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (paletteViewModel.uiState.value.palettes.isEmpty()) {
            paletteViewModel.loadPalettes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = R.string.palette_editor_title,
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = {
                    name = it
                    localError = null
                },
                label = { Text(stringResource(id = R.string.palette_name_hint)) },
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = colorsInput,
                onValueChange = {
                    colorsInput = it
                    localError = null
                },
                label = { Text(stringResource(id = R.string.palette_colors_hint)) },
            )

            localError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            if (existing == null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val parsed = parseHexColors(colorsInput)
                        if (parsed == null) {
                            localError = "Введите от 3 до 15 корректных HEX-цветов"
                            return@Button
                        }
                        paletteViewModel.createPalette(name = name, colors = parsed) {
                            onBack()
                        }
                    },
                ) {
                    Text(stringResource(id = R.string.create_palette))
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (name.isBlank()) {
                            localError = "Название палитры не может быть пустым"
                            return@Button
                        }
                        paletteViewModel.renamePalette(existing.id, name) {
                            onBack()
                        }
                    },
                ) {
                    Text(stringResource(id = R.string.rename_palette))
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        paletteViewModel.deletePalette(existing.id) {
                            onBack()
                        }
                    },
                ) {
                    Text(stringResource(id = R.string.delete_palette), color = MaterialTheme.colorScheme.error)
                }
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    }
}

private fun parseHexColors(raw: String): List<String>? {
    val colors = raw
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.uppercase() }

    if (colors.size !in 3..15) {
        return null
    }

    val regex = Regex("^#[0-9A-F]{6}$")
    if (colors.any { !regex.matches(it) }) {
        return null
    }

    return colors
}
