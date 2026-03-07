package ru.diamko.paleta.ui.palettes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.core.palette.HexColors
import ru.diamko.paleta.core.palette.RandomPaletteGenerator
import ru.diamko.paleta.ui.components.ColorWheelPicker
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaMessageBanner
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.paletaTextFieldColors
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaletteEditorScreen(
    paletteId: Long?,
    paletteViewModel: PaletteViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val existing = paletteId?.let { paletteViewModel.paletteById(it) }
    val isCreateMode = existing == null

    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var colorsInput by remember(existing?.id) { mutableStateOf(existing?.colors?.joinToString(",").orEmpty()) }
    var createColors by remember(existing?.id) {
        mutableStateOf(if (isCreateMode) RandomPaletteGenerator.generate(5) else emptyList())
    }
    var localError by remember { mutableStateOf<String?>(null) }
    var selectedColorIndex by remember(existing?.id) { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (paletteViewModel.uiState.value.palettes.isEmpty()) {
            paletteViewModel.loadPalettes()
        }
    }

    val parsedColors = remember(colorsInput, createColors, isCreateMode) {
        if (isCreateMode) {
            createColors
        } else {
            HexColors.parse(colorsInput) ?: emptyList()
        }
    }
    val safeSelectedColorIndex = selectedColorIndex.coerceIn(0, max(0, parsedColors.lastIndex))
    val selectedColorHex = parsedColors.getOrNull(safeSelectedColorIndex)

    fun updateColorAt(index: Int, rawHex: String) {
        if (index !in parsedColors.indices) return
        val normalized = ColorTools.hexToColorInt(rawHex)?.let(ColorTools::colorIntToHex) ?: return
        val updated = parsedColors.toMutableList()
        updated[index] = normalized
        if (isCreateMode) {
            createColors = updated
        } else {
            colorsInput = updated.joinToString(",")
        }
        localError = null
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
                        subtitle = stringResource(id = R.string.palette_editor_subtitle),
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

                    if (parsedColors.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            parsedColors.forEachIndexed { index, hex ->
                                val color = ColorTools.hexToColorInt(hex)?.let(::Color) ?: Color.Gray
                                val isSelected = index == safeSelectedColorIndex
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 40.dp else 34.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                Color.White
                                            } else {
                                                Color.White.copy(alpha = 0.8f)
                                            },
                                            shape = CircleShape,
                                        )
                                        .clickable { selectedColorIndex = index },
                                )
                            }
                        }
                    }

                    if (!isCreateMode) {
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
                    }

                    if (selectedColorHex != null) {
                        PaletaSectionTitle(
                            title = stringResource(id = R.string.color_wheel_title),
                            subtitle = stringResource(
                                id = R.string.color_wheel_subtitle,
                                safeSelectedColorIndex + 1,
                                selectedColorHex,
                            ),
                        )
                        ColorWheelPicker(
                            colorHex = selectedColorHex,
                            onColorChange = { updatedHex ->
                                updateColorAt(safeSelectedColorIndex, updatedHex)
                            },
                        )

                        ColorHarmonySection(
                            baseHex = selectedColorHex,
                            colorCount = parsedColors.size.coerceAtLeast(3),
                            onApply = { harmony ->
                                if (isCreateMode) {
                                    createColors = harmony
                                } else {
                                    colorsInput = harmony.joinToString(",")
                                }
                                selectedColorIndex = 0
                                localError = null
                            },
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.color_wheel_empty_hint),
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }

                    localError?.let {
                        PaletaMessageBanner(
                            message = it,
                            isError = true,
                        )
                    }

                    if (isCreateMode) {
                        PaletaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.create_palette),
                            onClick = {
                                val parsed = HexColors.normalize(parsedColors)
                                if (parsed == null) {
                                    localError = context.getString(R.string.palette_invalid_hex_count)
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
                                    localError = context.getString(R.string.palette_name_required)
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
