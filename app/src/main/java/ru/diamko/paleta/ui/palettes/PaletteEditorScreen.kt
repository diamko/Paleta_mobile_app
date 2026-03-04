package ru.diamko.paleta.ui.palettes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.core.palette.HexColors
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaMessageBanner
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.paletaTextFieldColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text(text = stringResource(id = R.string.palette_editor_title)) },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = if (existing == null) {
                            stringResource(id = R.string.create_palette)
                        } else {
                            stringResource(id = R.string.rename_palette)
                        },
                        subtitle = "Введите название и HEX цвета через запятую",
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = name,
                        onValueChange = {
                            name = it
                            localError = null
                        },
                        label = { Text(stringResource(id = R.string.palette_name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = paletaTextFieldColors(),
                    )

                    val parsedColors = remember(colorsInput) { HexColors.parse(colorsInput) }
                    if (!parsedColors.isNullOrEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            parsedColors.forEachIndexed { index, hex ->
                                val color = ColorTools.hexToColorInt(hex)?.let(::Color) ?: Color.Gray
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            shape = CircleShape,
                                        ),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = colorsInput,
                        onValueChange = {
                            colorsInput = it
                            localError = null
                        },
                        label = { Text(stringResource(id = R.string.palette_colors_hint)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = paletaTextFieldColors(),
                    )

                    localError?.let {
                        PaletaMessageBanner(
                            message = it,
                            isError = true,
                        )
                    }

                    if (existing == null) {
                        PaletaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.create_palette),
                            onClick = {
                                val parsed = HexColors.parse(colorsInput)
                                if (parsed == null) {
                                    localError = "Введите от 3 до 15 корректных HEX-цветов"
                                    return@PaletaPrimaryButton
                                }
                                paletteViewModel.createPalette(name = name, colors = parsed) {
                                    onBack()
                                }
                            },
                        )
                    } else {
                        PaletaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.rename_palette),
                            onClick = {
                                if (name.isBlank()) {
                                    localError = "Название палитры не может быть пустым"
                                    return@PaletaPrimaryButton
                                }
                                paletteViewModel.renamePalette(existing.id, name) {
                                    onBack()
                                }
                            },
                        )

                        PaletaGhostButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.delete_palette),
                            onClick = {
                                paletteViewModel.deletePalette(existing.id) {
                                    onBack()
                                }
                            },
                            isDanger = true,
                        )
                    }

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.cancel),
                        onClick = onBack,
                    )
                }
            }
        }
    }
}
