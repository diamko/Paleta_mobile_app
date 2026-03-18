package ru.diamko.paleta.ui.palettes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.core.palette.HexColors
import ru.diamko.paleta.core.palette.PaletteExportFormat
import ru.diamko.paleta.core.palette.RandomPaletteGenerator
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.ui.components.HorizontalScrollIndicator
import ru.diamko.paleta.ui.components.ColorCountDropdown
import ru.diamko.paleta.ui.components.ColorWheelPicker
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.PaletaTopBannerHost
import ru.diamko.paleta.ui.components.paletaTextFieldColors
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaletteEditorScreen(
    paletteId: Long?,
    paletteViewModel: PaletteViewModel,
    onBack: () -> Unit,
    isAuthenticated: Boolean,
    onRequireLogin: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = paletteId?.let { paletteViewModel.paletteById(it) }
    val isCreateMode = existing == null

    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var colorsInput by remember(existing?.id) { mutableStateOf(existing?.colors?.joinToString(",").orEmpty()) }
    var createColors by remember(existing?.id) {
        mutableStateOf(if (isCreateMode) RandomPaletteGenerator.generate(5) else emptyList())
    }
    var localError by remember { mutableStateOf<String?>(null) }
    var selectedColorIndex by remember(existing?.id) { mutableStateOf(0) }
    var selectedFormat by remember { mutableStateOf(PaletteExportFormat.JSON) }
    var pendingExport by remember { mutableStateOf<PaletteExportFile?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { outputUri ->
        val payload = pendingExport ?: return@rememberLauncherForActivityResult
        if (outputUri == null) {
            localError = context.getString(R.string.export_canceled)
            pendingExport = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            writeExportFile(context, outputUri, payload)
                .onSuccess {
                    localError = null
                    pendingExport = null
                }
                .onFailure { error ->
                    localError = error.message ?: context.getString(R.string.export_error_generic)
                    pendingExport = null
                }
        }
    }

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

    var harmonyBaseHex by remember { mutableStateOf(selectedColorHex ?: "#000000") }
    var harmonyColors by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(selectedColorHex) {
        if (selectedColorHex != null) {
            harmonyBaseHex = selectedColorHex
        }
    }

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

    fun resizeCreateColors(targetCount: Int) {
        if (!isCreateMode) return
        val safeTarget = targetCount.coerceIn(3, 15)
        val current = createColors
        createColors = if (current.size >= safeTarget) {
            current.take(safeTarget)
        } else {
            buildList {
                addAll(current)
                while (size < safeTarget) {
                    add(RandomPaletteGenerator.generate(1).firstOrNull() ?: "#000000")
                }
            }
        }
        selectedColorIndex = selectedColorIndex.coerceIn(0, max(0, createColors.lastIndex))
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PaletaCard(modifier = Modifier.fillMaxWidth()) {
                        PaletaSectionTitle(
                            title = if (existing == null) {
                                stringResource(id = R.string.create_palette)
                            } else {
                                stringResource(id = R.string.save_changes)
                            },
                            subtitle = if (isCreateMode) {
                                stringResource(id = R.string.palette_editor_subtitle_create)
                            } else {
                                stringResource(id = R.string.palette_editor_subtitle)
                            },
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
                        if (isCreateMode) {
                            ColorCountDropdown(
                                label = stringResource(id = R.string.color_count_hint),
                                selectedCount = createColors.size.coerceIn(3, 15),
                                onSelected = { selected ->
                                    if (selected != null) {
                                        resizeCreateColors(selected)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            parsedColors.forEachIndexed { index, hex ->
                                val color = ColorTools.hexToColorInt(hex)?.let(::Color) ?: Color.Gray
                                val isSelected = index == safeSelectedColorIndex
                                val size = if (isSelected) 40.dp else 34.dp
                                val borderWidth = if (isSelected) 2.dp else 1.dp
                                Canvas(
                                    modifier = Modifier
                                        .size(size)
                                        .clickable { selectedColorIndex = index },
                                ) {
                                    val radius = this.size.minDimension / 2f
                                    val borderWidthPx = borderWidth.toPx()
                                    // Draw background circle
                                    drawCircle(
                                        color = color,
                                        radius = radius,
                                    )
                                    // Draw inner border
                                    drawCircle(
                                        color = if (isSelected) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.8f)
                                        },
                                        radius = radius - borderWidthPx,
                                        style = Stroke(width = borderWidthPx),
                                    )
                                }
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
                        ColorHarmonySection(
                            baseHex = harmonyBaseHex,
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
                            onColorsGenerated = { harmonyColors = it },
                        )

                        PaletaSectionTitle(
                            title = stringResource(id = R.string.color_wheel_title),
                            subtitle = stringResource(
                                id = R.string.color_wheel_subtitle,
                                safeSelectedColorIndex + 1,
                                selectedColorHex,
                            ),
                        )
                        ColorWheelPicker(
                            colorHex = harmonyBaseHex,
                            onColorChange = { updatedHex ->
                                val currentIndex = selectedColorIndex.coerceIn(0, max(0, parsedColors.lastIndex))
                                harmonyBaseHex = updatedHex
                                updateColorAt(currentIndex, updatedHex)
                            },
                            harmonyColors = harmonyColors,
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.color_wheel_empty_hint),
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }

                    PaletaSectionTitle(
                        title = stringResource(id = R.string.export_title),
                        subtitle = stringResource(id = R.string.export_subtitle),
                    )
                    val exportScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier.horizontalScroll(exportScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaletteExportFormat.entries.forEach { format ->
                            FilterChip(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format },
                                label = { Text(format.name) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                    HorizontalScrollIndicator(scrollState = exportScrollState)
                    PaletaPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.export_file),
                        enabled = !isBusy && parsedColors.isNotEmpty(),
                        onClick = {
                            val colors = HexColors.normalize(parsedColors)
                            if (colors == null) {
                                localError = context.getString(R.string.palette_invalid_hex_count)
                                return@PaletaPrimaryButton
                            }
                            isBusy = true
                            paletteViewModel.exportPalette(
                                name = name.ifBlank { context.getString(R.string.palette_editor_title) },
                                colors = colors,
                                format = selectedFormat.ext,
                                onDone = { payload ->
                                    isBusy = false
                                    pendingExport = payload
                                    createDocumentLauncher.launch(payload.fileName)
                                },
                                onError = { error ->
                                    isBusy = false
                                    localError = error
                                },
                            )
                        },
                    )

                    if (isCreateMode) {
                        PaletaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.create_palette),
                            onClick = {
                                if (!isAuthenticated) {
                                    localError = context.getString(R.string.login_to_save_palette)
                                    onRequireLogin()
                                } else {
                                    val parsed = HexColors.normalize(parsedColors)
                                    if (parsed == null) {
                                        localError = context.getString(R.string.palette_invalid_hex_count)
                                        return@PaletaPrimaryButton
                                    }
                                    paletteViewModel.createPalette(name = name, colors = parsed) {
                                        onBack()
                                    }
                                }
                            },
                        )
                    } else {
                        PaletaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.save_changes),
                            onClick = {
                                if (!isAuthenticated) {
                                    localError = context.getString(R.string.login_to_save_palette)
                                    onRequireLogin()
                                    return@PaletaPrimaryButton
                                }
                                val parsed = HexColors.parse(colorsInput)
                                if (name.isBlank()) {
                                    localError = context.getString(R.string.palette_name_required)
                                    return@PaletaPrimaryButton
                                }
                                if (parsed == null) {
                                    localError = context.getString(R.string.palette_invalid_hex_count)
                                    return@PaletaPrimaryButton
                                }
                                paletteViewModel.savePaletteChanges(
                                    id = existing!!.id,
                                    newName = name,
                                    newColors = parsed,
                                ) {
                                    onBack()
                                }
                            },
                        )

                        PaletaGhostButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.delete_palette),
                            onClick = {
                                if (!isAuthenticated) {
                                    localError = context.getString(R.string.login_to_save_palette)
                                    onRequireLogin()
                                } else {
                                    paletteViewModel.deletePalette(existing!!.id) {
                                        onBack()
                                    }
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

                PaletaTopBannerHost(
                    error = localError,
                    info = null,
                )
            }
        }
    }
}
