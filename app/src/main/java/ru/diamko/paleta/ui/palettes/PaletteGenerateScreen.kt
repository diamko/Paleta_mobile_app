package ru.diamko.paleta.ui.palettes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalClipboardManager

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
        markerPositions = List(normalized.size) { index -> markerPositions.getOrNull(index) }
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
            statusMessage = null

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

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
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
                localError?.let { message ->
                    PaletaMessageBanner(message = message, isError = true)
                }
                statusMessage?.let { message ->
                    PaletaMessageBanner(message = message, isError = false)
                }

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
                            subtitle = "Выберите цвет ниже и тапните по нужной точке на фото",
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .onSizeChanged { imageBoxSize = it }
                                .pointerInput(bitmap, safeSelectedIndex, paletteColors, fitMetrics) {
                                    detectTapGestures { tapOffset ->
                                        val metrics = fitMetrics ?: return@detectTapGestures
                                        val sampled = sampleColorAtTap(
                                            bitmap = bitmap,
                                            tapOffset = tapOffset,
                                            metrics = metrics,
                                        ) ?: run {
                                            localError = "Тапните по области изображения"
                                            return@detectTapGestures
                                        }
                                        updateColorAt(safeSelectedIndex, sampled.hex)
                                        val updatedMarkers = markerPositions.toMutableList()
                                        while (updatedMarkers.size < paletteColors.size) {
                                            updatedMarkers.add(null)
                                        }
                                        updatedMarkers[safeSelectedIndex] = Offset(sampled.xNorm, sampled.yNorm)
                                        markerPositions = updatedMarkers
                                        localError = null
                                        statusMessage = "Цвет ${safeSelectedIndex + 1} обновлен из изображения"
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
                                    val markerX = metrics.left + marker.x * metrics.width
                                    val markerY = metrics.top + marker.y * metrics.height
                                    val color = paletteColors.getOrNull(index)?.let {
                                        ColorTools.hexToColorInt(it)?.let(::Color)
                                    } ?: Color.Gray
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    x = (markerX - 13f).roundToInt(),
                                                    y = (markerY - 13f).roundToInt(),
                                                )
                                            }
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (index == safeSelectedIndex) 3.dp else 2.dp,
                                                color = if (index == safeSelectedIndex) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.White
                                                },
                                                shape = CircleShape,
                                            ),
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Подсказка: сначала нажмите на карточку цвета, затем выберите точку на фото",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = "Цвета палитры",
                        subtitle = "Карточки как в веб-версии: каждый цвет редактируется отдельно",
                    )

                    if (!hasPalette) {
                        Text(
                            text = "Сначала сгенерируйте палитру или извлеките цвета из изображения",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            paletteColors.forEachIndexed { index, hex ->
                                val color = ColorTools.hexToColorInt(hex)?.let(::Color) ?: Color.Gray
                                val selected = index == safeSelectedIndex
                                Column(
                                    modifier = Modifier
                                        .width(104.dp)
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
                                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
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
                                    Text(
                                        text = hex,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
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

private fun sampleColorAtTap(
    bitmap: Bitmap,
    tapOffset: Offset,
    metrics: ImageFitMetrics,
): SampledPoint? {
    if (tapOffset.x < metrics.left || tapOffset.y < metrics.top) return null
    if (tapOffset.x > metrics.left + metrics.width || tapOffset.y > metrics.top + metrics.height) return null

    val xNorm = ((tapOffset.x - metrics.left) / metrics.width).coerceIn(0f, 1f)
    val yNorm = ((tapOffset.y - metrics.top) / metrics.height).coerceIn(0f, 1f)

    val x = (xNorm * (bitmap.width - 1)).roundToInt().coerceIn(0, bitmap.width - 1)
    val y = (yNorm * (bitmap.height - 1)).roundToInt().coerceIn(0, bitmap.height - 1)
    val colorInt = bitmap.getPixel(x, y)
    return SampledPoint(
        hex = ColorTools.colorIntToHex(colorInt),
        xNorm = xNorm,
        yNorm = yNorm,
    )
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
