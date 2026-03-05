package ru.diamko.paleta.ui.palettes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.core.palette.HexColors
import ru.diamko.paleta.core.palette.PaletteExportFormat
import ru.diamko.paleta.core.palette.RandomPaletteGenerator
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaMessageBanner
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.paletaTextFieldColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class ImageFitMetrics(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private data class SampledPoint(
    val hex: String,
    val xNorm: Float,
    val yNorm: Float,
    val pixelX: Int,
    val pixelY: Int,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaletteGenerateScreen(
    paletteViewModel: PaletteViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBoxSize by remember { mutableStateOf(IntSize.Zero) }

    var paletteName by remember { mutableStateOf("Моя палитра") }
    var colorCountRaw by remember { mutableStateOf("5") }
    var paletteColors by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var markerPositions by remember { mutableStateOf<List<Offset?>>(emptyList()) }
    var isDraggingPipette by remember { mutableStateOf(false) }
    var loupeTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var loupeSample by remember { mutableStateOf<SampledPoint?>(null) }

    var localError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(PaletteExportFormat.JSON) }
    var pendingExport by remember { mutableStateOf<PaletteExportFile?>(null) }

    fun applyPalette(colors: List<String>, message: String) {
        val normalized = HexColors.normalize(colors)
        if (normalized == null) {
            localError = "Некорректная палитра (нужно от 3 до 15 HEX-цветов)"
            return
        }
        paletteColors = normalized
        selectedColorIndex = selectedColorIndex.coerceIn(0, normalized.lastIndex)
        markerPositions = List(normalized.size) { idx -> markerPositions.getOrNull(idx) }
        statusMessage = message
        localError = null
    }

    fun updateColorAt(index: Int, colorHex: String) {
        if (index !in paletteColors.indices) return
        val normalized = normalizeSingleHex(colorHex) ?: return
        val updated = paletteColors.toMutableList()
        updated[index] = normalized
        paletteColors = updated
    }

    fun paletteSize(): Int {
        return colorCountRaw.toIntOrNull()?.coerceIn(3, 15) ?: 5
    }

    fun extractFromImage(uri: Uri) {
        scope.launch {
            isBusy = true
            localError = null
            statusMessage = "Извлечение цветов..."
            paletteColors = emptyList()
            markerPositions = emptyList()
            selectedColorIndex = 0
            loupeTouchPosition = null
            loupeSample = null

            val data = withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                bytes to bitmap
            }
            val bytes = data.first
            val bitmap = data.second
            if (bytes == null || bytes.isEmpty() || bitmap == null) {
                isBusy = false
                localError = "Не удалось прочитать изображение"
                return@launch
            }

            imageBitmap = bitmap
            val fileName = resolveFileName(context, uri)
            paletteViewModel.generateFromImage(
                fileName = fileName,
                imageBytes = bytes,
                colorCount = paletteSize(),
                onDone = { colors ->
                    isBusy = false
                    if (colors.isEmpty()) {
                        localError = "Не удалось извлечь цвета из изображения"
                        return@generateFromImage
                    }
                    applyPalette(colors, "Извлечено цветов: ${colors.size}")
                    markerPositions = estimateInitialMarkerPositions(bitmap, colors)
                },
                onError = { error ->
                    isBusy = false
                    localError = error
                },
            )
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { outputUri ->
        val payload = pendingExport ?: return@rememberLauncherForActivityResult
        if (outputUri == null) {
            statusMessage = "Экспорт отменен"
            pendingExport = null
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                    stream.write(payload.bytes)
                } ?: error("Не удалось открыть файл для записи")
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    statusMessage = "Файл сохранен: ${payload.fileName}"
                    localError = null
                    pendingExport = null
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    localError = error.message ?: "Ошибка экспорта"
                    pendingExport = null
                }
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        extractFromImage(uri)
    }

    val bitmap = imageBitmap
    val hasPalette = paletteColors.isNotEmpty()
    val safeSelectedIndex = selectedColorIndex.coerceIn(0, max(0, paletteColors.lastIndex))
    val selectedHex = paletteColors.getOrNull(safeSelectedIndex) ?: "#000000"
    val selectedColorInt = ColorTools.hexToColorInt(selectedHex) ?: AndroidColor.BLACK

    val fitMetrics = remember(bitmap, imageBoxSize) {
        bitmap?.let {
            calculateImageFitMetrics(
                container = imageBoxSize,
                imageWidth = it.width,
                imageHeight = it.height,
            )
        }
    }

    fun updateColorFromImagePoint(
        position: Offset,
        colorIndex: Int = safeSelectedIndex,
        touchPosition: Offset = position,
    ) {
        if (colorIndex !in paletteColors.indices) return
        val currentBitmap = bitmap ?: return
        val metrics = fitMetrics ?: return
        val sampled = sampleColorAtPosition(
            bitmap = currentBitmap,
            position = position,
            metrics = metrics,
        )
        updateColorAt(colorIndex, sampled.hex)
        val updatedMarkers = markerPositions.toMutableList()
        while (updatedMarkers.size < paletteColors.size) {
            updatedMarkers.add(null)
        }
        updatedMarkers[colorIndex] = Offset(sampled.xNorm, sampled.yNorm)
        markerPositions = updatedMarkers
        loupeTouchPosition = touchPosition
        loupeSample = sampled
        localError = null
    }

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text(stringResource(id = R.string.generate_palette_title)) },
                    actions = {
                        PaletaGhostButton(
                            modifier = Modifier.padding(end = 12.dp),
                            text = stringResource(id = R.string.back),
                            onClick = onBack,
                        )
                    },
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
                localError?.let { PaletaMessageBanner(message = it, isError = true) }
                statusMessage?.let { PaletaMessageBanner(message = it, isError = false) }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = "Источник палитры",
                        subtitle = "Случайная генерация или извлечение цветов из изображения",
                    )

                    OutlinedTextField(
                        value = paletteName,
                        onValueChange = {
                            paletteName = it
                            localError = null
                        },
                        label = { Text(stringResource(id = R.string.palette_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = paletaTextFieldColors(),
                    )

                    OutlinedTextField(
                        value = colorCountRaw,
                        onValueChange = {
                            colorCountRaw = it.filter(Char::isDigit).take(2)
                            localError = null
                        },
                        label = { Text(stringResource(id = R.string.color_count_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = paletaTextFieldColors(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaletaPrimaryButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.generate_random),
                            onClick = {
                                applyPalette(
                                    colors = RandomPaletteGenerator.generate(paletteSize()),
                                    message = "Сгенерирована случайная палитра",
                                )
                            },
                            enabled = !isBusy,
                        )

                        PaletaGhostButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.pick_image),
                            onClick = { pickImageLauncher.launch("image/*") },
                            enabled = !isBusy,
                        )
                    }
                }

                if (bitmap != null) {
                    PaletaCard(modifier = Modifier.fillMaxWidth()) {
                        PaletaSectionTitle(
                            title = "Пипетка по изображению",
                            subtitle = "Выберите цвет ниже и ведите пальцем по фото",
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .onSizeChanged { imageBoxSize = it }
                                .pointerInput(bitmap, fitMetrics) {
                                    awaitEachGesture {
                                        val metrics = fitMetrics ?: return@awaitEachGesture
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val draggingIndex = findMarkerIndexAtPosition(
                                            position = down.position,
                                            markerPositions = markerPositions,
                                            metrics = metrics,
                                        ) ?: return@awaitEachGesture
                                        if (draggingIndex != safeSelectedIndex) {
                                            selectedColorIndex = draggingIndex
                                            statusMessage = "Р’С‹Р±СЂР°РЅР° РїРёРїРµС‚РєР°: Р¦РІРµС‚ ${draggingIndex + 1}"
                                            localError = null
                                        }
                                        val markerNorm = markerPositions.getOrNull(draggingIndex)
                                        val markerAnchor = markerNorm?.let {
                                            Offset(
                                                x = metrics.left + it.x * metrics.width,
                                                y = metrics.top + it.y * metrics.height,
                                            )
                                        } ?: down.position
                                        val touchOffset = down.position - markerAnchor
                                        isDraggingPipette = true
                                        updateColorFromImagePoint(
                                            position = markerAnchor,
                                            colorIndex = draggingIndex,
                                            touchPosition = down.position,
                                        )
                                        var pointerId = down.id

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId }
                                                ?: event.changes.firstOrNull()
                                                ?: break
                                            pointerId = change.id
                                            if (!change.pressed) {
                                                break
                                            }
                                            updateColorFromImagePoint(
                                                position = change.position - touchOffset,
                                                colorIndex = draggingIndex,
                                                touchPosition = change.position,
                                            )
                                            change.consume()
                                        }

                                        isDraggingPipette = false
                                        statusMessage = "Цвет ${safeSelectedIndex + 1} обновлен из изображения"
                                        loupeTouchPosition = null
                                        loupeSample = null
                                    }
                                },
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Выбранное изображение",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )

                            val metrics = fitMetrics
                            if (metrics != null) {
                                markerPositions.forEachIndexed { index, marker ->
                                    if (marker == null) return@forEachIndexed
                                    if (isDraggingPipette && index == safeSelectedIndex) {
                                        return@forEachIndexed
                                    }
                                    val markerX = metrics.left + marker.x * metrics.width
                                    val markerY = metrics.top + marker.y * metrics.height
                                    val markerColor = paletteColors.getOrNull(index)?.let {
                                        ColorTools.hexToColorInt(it)?.let(::Color)
                                    } ?: Color.Gray

                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    x = markerX.roundToInt(),
                                                    y = markerY.roundToInt(),
                                                )
                                            }
                                            .size(34.dp)
                                            .clickable(enabled = false) {
                                                selectedColorIndex = index
                                                statusMessage = "Выбрана пипетка: Цвет ${index + 1}"
                                                localError = null
                                            },
                                    ) {
                                        PipetteMarker(
                                            color = markerColor,
                                            selected = index == safeSelectedIndex,
                                            dragging = isDraggingPipette && index == safeSelectedIndex,
                                        )
                                    }
                                }
                            }

                            if (isDraggingPipette && loupeTouchPosition != null && loupeSample != null) {
                                val touch = loupeTouchPosition ?: Offset.Zero
                                val sample = loupeSample ?: SampledPoint(
                                    hex = "#000000",
                                    xNorm = 0f,
                                    yNorm = 0f,
                                    pixelX = 0,
                                    pixelY = 0,
                                )
                                ColorLoupe(
                                    modifier = Modifier
                                        .offset {
                                            calculateLoupeOffset(
                                                anchor = touch,
                                                containerSize = imageBoxSize,
                                            )
                                        },
                                    bitmap = bitmap,
                                    sample = sample,
                                )
                            }
                        }

                        Text(
                            text = "Пипетки появляются сразу после извлечения. Лупа показывается только при выборе и движении активной пипетки.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = "Цвета палитры",
                        subtitle = "Максимум 3 карточки в ряд, редактирование каждого цвета отдельно",
                    )

                    if (!hasPalette) {
                        Text(
                            text = "Сначала сгенерируйте палитру или извлеките цвета из изображения",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            paletteColors.forEachIndexed { index, hex ->
                                val color = ColorTools.hexToColorInt(hex)?.let(::Color) ?: Color.Gray
                                val selected = index == safeSelectedIndex
                                Column(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            },
                                        )
                                        .clickable { selectedColorIndex = index }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color)
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(12.dp),
                                            ),
                                    )
                                    Text(
                                        text = "Цвет ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(text = hex, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        PaletaSectionTitle(
                            title = "Редактор выбранного цвета",
                            subtitle = "Выбран: Цвет ${safeSelectedIndex + 1} ($selectedHex)",
                        )

                        ColorChannelSlider(
                            title = "R",
                            value = AndroidColor.red(selectedColorInt),
                            onValueChange = { red ->
                                val colorInt = AndroidColor.rgb(
                                    red,
                                    AndroidColor.green(selectedColorInt),
                                    AndroidColor.blue(selectedColorInt),
                                )
                                updateColorAt(safeSelectedIndex, ColorTools.colorIntToHex(colorInt))
                            },
                        )
                        ColorChannelSlider(
                            title = "G",
                            value = AndroidColor.green(selectedColorInt),
                            onValueChange = { green ->
                                val colorInt = AndroidColor.rgb(
                                    AndroidColor.red(selectedColorInt),
                                    green,
                                    AndroidColor.blue(selectedColorInt),
                                )
                                updateColorAt(safeSelectedIndex, ColorTools.colorIntToHex(colorInt))
                            },
                        )
                        ColorChannelSlider(
                            title = "B",
                            value = AndroidColor.blue(selectedColorInt),
                            onValueChange = { blue ->
                                val colorInt = AndroidColor.rgb(
                                    AndroidColor.red(selectedColorInt),
                                    AndroidColor.green(selectedColorInt),
                                    blue,
                                )
                                updateColorAt(safeSelectedIndex, ColorTools.colorIntToHex(colorInt))
                            },
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PaletaGhostButton(
                                modifier = Modifier.weight(1f),
                                text = "Копировать HEX",
                                onClick = {
                                    clipboard.setText(AnnotatedString(selectedHex))
                                    statusMessage = "HEX скопирован: $selectedHex"
                                },
                            )
                            PaletaGhostButton(
                                modifier = Modifier.weight(1f),
                                text = "Случайный цвет",
                                onClick = {
                                    val random = ColorTools.colorIntToHex(
                                        AndroidColor.rgb(
                                            (0..255).random(),
                                            (0..255).random(),
                                            (0..255).random(),
                                        ),
                                    )
                                    updateColorAt(safeSelectedIndex, random)
                                },
                            )
                        }
                    }
                }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = "Экспорт и сохранение",
                        subtitle = "Выберите формат и действие",
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaletaPrimaryButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.export_file),
                            onClick = {
                                val colors = HexColors.normalize(paletteColors)
                                if (colors == null) {
                                    localError = "Палитра должна содержать от 3 до 15 корректных цветов"
                                    return@PaletaPrimaryButton
                                }
                                isBusy = true
                                paletteViewModel.exportPalette(
                                    name = paletteName,
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
                            enabled = !isBusy,
                        )
                        PaletaGhostButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.save_palette),
                            onClick = {
                                val colors = HexColors.normalize(paletteColors)
                                if (colors == null) {
                                    localError = "Палитра должна содержать от 3 до 15 корректных цветов"
                                    return@PaletaGhostButton
                                }
                                paletteViewModel.createPalette(
                                    name = paletteName,
                                    colors = colors,
                                )
                                statusMessage = "Палитра сохранена"
                                localError = null
                            },
                            enabled = !isBusy,
                        )
                    }
                }

                if (isBusy) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$title: $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
        )
    }
}

@Composable
private fun PipetteMarker(
    color: Color,
    selected: Boolean,
    dragging: Boolean,
) {
    val accent = if (selected) MaterialTheme.colorScheme.primary else Color.White
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(10.dp),
        ) {
            val triangle = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(triangle, color = accent)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 3.dp, y = 3.dp)
                .size(if (dragging) 26.dp else 24.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 2.dp,
                    color = accent,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun ColorLoupe(
    modifier: Modifier = Modifier,
    bitmap: Bitmap,
    sample: SampledPoint,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Column(
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xDD10141E))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
        ) {
            val zoomRadius = 8
            val srcSize = zoomRadius * 2 + 1
            val srcLeft = (sample.pixelX - zoomRadius).coerceIn(0, bitmap.width - srcSize)
            val srcTop = (sample.pixelY - zoomRadius).coerceIn(0, bitmap.height - srcSize)

            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset(srcLeft, srcTop),
                srcSize = IntSize(srcSize, srcSize),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            )

            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 1.4f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 1.4f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = 4f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.4f),
            )
        }

        Text(
            text = sample.hex,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            textAlign = TextAlign.Center,
        )

        Canvas(modifier = Modifier.size(10.dp, 8.dp)) {
            val tail = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(tail, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

private fun normalizeSingleHex(raw: String): String? {
    val trimmed = raw.trim().uppercase()
    val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    val colorInt = ColorTools.hexToColorInt(withHash) ?: return null
    return ColorTools.colorIntToHex(colorInt)
}

private fun calculateImageFitMetrics(
    container: IntSize,
    imageWidth: Int,
    imageHeight: Int,
): ImageFitMetrics? {
    if (container.width <= 0 || container.height <= 0 || imageWidth <= 0 || imageHeight <= 0) {
        return null
    }
    val scale = min(
        container.width.toFloat() / imageWidth.toFloat(),
        container.height.toFloat() / imageHeight.toFloat(),
    )
    val drawWidth = imageWidth * scale
    val drawHeight = imageHeight * scale
    val left = (container.width - drawWidth) / 2f
    val top = (container.height - drawHeight) / 2f
    return ImageFitMetrics(left = left, top = top, width = drawWidth, height = drawHeight)
}

private fun sampleColorAtPosition(
    bitmap: Bitmap,
    position: Offset,
    metrics: ImageFitMetrics,
): SampledPoint {
    val xNorm = ((position.x - metrics.left) / metrics.width).coerceIn(0f, 1f)
    val yNorm = ((position.y - metrics.top) / metrics.height).coerceIn(0f, 1f)

    val x = (xNorm * (bitmap.width - 1)).roundToInt().coerceIn(0, bitmap.width - 1)
    val y = (yNorm * (bitmap.height - 1)).roundToInt().coerceIn(0, bitmap.height - 1)
    val colorInt = bitmap.getPixel(x, y)
    return SampledPoint(
        hex = ColorTools.colorIntToHex(colorInt),
        xNorm = xNorm,
        yNorm = yNorm,
        pixelX = x,
        pixelY = y,
    )
}

private fun findMarkerIndexAtPosition(
    position: Offset,
    markerPositions: List<Offset?>,
    metrics: ImageFitMetrics,
): Int? {
    val circleOffset = 3f
    val circleSize = 24f
    val hitPadding = 8f
    return markerPositions.indices
        .reversed()
        .firstOrNull { index ->
            val marker = markerPositions[index] ?: return@firstOrNull false
            val markerLeft = metrics.left + marker.x * metrics.width
            val markerTop = metrics.top + marker.y * metrics.height
            val centerX = markerLeft + circleOffset + circleSize / 2f
            val centerY = markerTop + circleOffset + circleSize / 2f
            val radius = circleSize / 2f + hitPadding
            val dx = position.x - centerX
            val dy = position.y - centerY
            dx * dx + dy * dy <= radius * radius
        }
}

private fun calculateLoupeOffset(
    anchor: Offset,
    containerSize: IntSize,
): IntOffset {
    val loupeWidth = 72
    val loupeHeight = 86
    val margin = 18

    var x = anchor.x.roundToInt() + margin
    var y = anchor.y.roundToInt() - loupeHeight - margin

    if (x + loupeWidth > containerSize.width) {
        x = anchor.x.roundToInt() - loupeWidth - margin
    }
    if (y < 0) {
        y = anchor.y.roundToInt() + margin
    }

    val maxX = (containerSize.width - loupeWidth).coerceAtLeast(0)
    val maxY = (containerSize.height - loupeHeight).coerceAtLeast(0)

    return IntOffset(
        x = x.coerceIn(0, maxX),
        y = y.coerceIn(0, maxY),
    )
}

private fun estimateInitialMarkerPositions(
    bitmap: Bitmap,
    colors: List<String>,
): List<Offset?> {
    if (colors.isEmpty() || bitmap.width <= 0 || bitmap.height <= 0) {
        return emptyList()
    }

    data class SamplePoint(
        val x: Int,
        val y: Int,
        val r: Int,
        val g: Int,
        val b: Int,
    )

    val stepX = max(1, bitmap.width / 160)
    val stepY = max(1, bitmap.height / 160)
    val samples = ArrayList<SamplePoint>()

    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val color = bitmap.getPixel(x, y)
            samples.add(
                SamplePoint(
                    x = x,
                    y = y,
                    r = AndroidColor.red(color),
                    g = AndroidColor.green(color),
                    b = AndroidColor.blue(color),
                ),
            )
            x += stepX
        }
        y += stepY
    }

    if (samples.isEmpty()) {
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val c = bitmap.getPixel(centerX, centerY)
        samples.add(
            SamplePoint(
                x = centerX,
                y = centerY,
                r = AndroidColor.red(c),
                g = AndroidColor.green(c),
                b = AndroidColor.blue(c),
            ),
        )
    }

    val usedPoints = mutableListOf<Offset>()
    val minDistancePx = max(10f, min(bitmap.width, bitmap.height) * 0.045f)
    val minDistanceSq = minDistancePx * minDistancePx

    return colors.map { hex ->
        val targetColor = ColorTools.hexToColorInt(hex) ?: return@map null
        val tr = AndroidColor.red(targetColor)
        val tg = AndroidColor.green(targetColor)
        val tb = AndroidColor.blue(targetColor)

        var best: SamplePoint? = null
        var bestScore = Long.MAX_VALUE

        for (sample in samples) {
            val dr = sample.r - tr
            val dg = sample.g - tg
            val db = sample.b - tb
            var score = (dr * dr + dg * dg + db * db).toLong()

            for (used in usedPoints) {
                val dx = sample.x.toFloat() - used.x
                val dy = sample.y.toFloat() - used.y
                val d2 = dx * dx + dy * dy
                if (d2 < minDistanceSq) {
                    score += ((minDistanceSq - d2) * 8f).toLong()
                }
            }

            if (score < bestScore) {
                bestScore = score
                best = sample
            }
        }

        val chosen = best ?: return@map null
        usedPoints.add(Offset(chosen.x.toFloat(), chosen.y.toFloat()))

        val xNorm = if (bitmap.width > 1) {
            chosen.x.toFloat() / (bitmap.width - 1).toFloat()
        } else {
            0f
        }
        val yNorm = if (bitmap.height > 1) {
            chosen.y.toFloat() / (bitmap.height - 1).toFloat()
        } else {
            0f
        }

        Offset(xNorm, yNorm)
    }
}

private fun resolveFileName(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            val name = cursor.getString(index)?.trim()
            if (!name.isNullOrBlank()) return name
        }
    }
    return "upload.jpg"
}
