/**
 * Модуль: PaletteGenerateScreen.
 * Назначение: Экран генерации палитры: случайная, из изображения, пипетка.
 */
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.core.palette.HexColors
import ru.diamko.paleta.core.palette.PaletteExportFormat
import ru.diamko.paleta.core.palette.RandomPaletteGenerator
import ru.diamko.paleta.core.storage.LastImageStorage
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.ui.components.HorizontalScrollIndicator
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.PaletaTopBannerHost
import ru.diamko.paleta.ui.components.ColorCountDropdown
import ru.diamko.paleta.ui.components.paletaTextFieldColors
import ru.diamko.paleta.ui.components.innerBorder
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

enum class PaletteGenerateScreenMode {
    RANDOM,
    IMAGE,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaletteGenerateScreen(
    paletteViewModel: PaletteViewModel,
    mode: PaletteGenerateScreenMode,
    onBack: () -> Unit,
    isAuthenticated: Boolean,
    onRequireLogin: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val appContext = context.applicationContext
    val lastImageStorage = remember(appContext) { LastImageStorage(appContext) }

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBoxSize by remember { mutableStateOf(IntSize.Zero) }

    var paletteName by remember { mutableStateOf(context.getString(R.string.new_palette)) }
    var colorCount by remember { mutableStateOf(5) }
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
    val paletteState by paletteViewModel.uiState.collectAsStateWithLifecycle()

    fun applyPalette(colors: List<String>, message: String) {
        val normalized = HexColors.normalize(colors)
        if (normalized == null) {
            localError = context.getString(R.string.palette_invalid_hex_count)
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
        return colorCount.coerceIn(3, 15)
    }

    suspend fun restoreLastImageIfAvailable() {
        if (mode != PaletteGenerateScreenMode.IMAGE || imageBitmap != null) return
        val bytes = lastImageStorage.read() ?: return
        if (bytes.isEmpty()) return
        val bitmap = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } ?: run {
            localError = context.getString(R.string.last_image_restore_failed)
            return
        }

        imageBitmap = bitmap
        isBusy = true
        localError = null
        statusMessage = context.getString(R.string.last_image_restored)
        paletteColors = emptyList()
        markerPositions = emptyList()
        selectedColorIndex = 0
        loupeTouchPosition = null
        loupeSample = null

        paletteViewModel.generateFromImage(
            fileName = "last_image.jpg",
            imageBytes = bytes,
            colorCount = paletteSize(),
            onDone = { colors ->
                isBusy = false
                if (colors.isEmpty()) {
                    localError = context.getString(R.string.extract_colors_failed)
                    return@generateFromImage
                }
                applyPalette(colors, context.getString(R.string.extracted_colors_count, colors.size))
                scope.launch {
                    val markers = withContext(Dispatchers.Default) {
                        estimateInitialMarkerPositions(bitmap, colors)
                    }
                    markerPositions = markers
                }
            },
            onError = { error ->
                isBusy = false
                localError = error
            },
        )
    }

    LaunchedEffect(mode) {
        if (mode == PaletteGenerateScreenMode.IMAGE) {
            paletteViewModel.loadRecentUploads()
            restoreLastImageIfAvailable()
        }
    }

    fun extractFromImage(uri: Uri) {
        scope.launch {
            isBusy = true
            localError = null
            statusMessage = context.getString(R.string.extracting_colors)
            paletteColors = emptyList()
            markerPositions = emptyList()
            selectedColorIndex = 0
            loupeTouchPosition = null
            loupeSample = null

            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) {
                isBusy = false
                localError = context.getString(R.string.read_image_failed)
                return@launch
            }
            val bitmap = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            if (bitmap == null) {
                isBusy = false
                localError = context.getString(R.string.read_image_failed)
                return@launch
            }
            lastImageStorage.save(bytes)

            imageBitmap = bitmap
            val fileName = resolveFileName(context, uri)
            paletteViewModel.generateFromImage(
                fileName = fileName,
                imageBytes = bytes,
                colorCount = paletteSize(),
                onDone = { colors ->
                    isBusy = false
                    if (colors.isEmpty()) {
                        localError = context.getString(R.string.extract_colors_failed)
                        return@generateFromImage
                    }
                    applyPalette(colors, context.getString(R.string.extracted_colors_count, colors.size))
                    scope.launch {
                        val markers = withContext(Dispatchers.Default) {
                            estimateInitialMarkerPositions(bitmap, colors)
                        }
                        markerPositions = markers
                    }
                },
                onError = { error ->
                    isBusy = false
                    localError = error
                },
            )
        }
    }

    fun useRecentUpload(imageUrl: String, fileName: String) {
        scope.launch {
            isBusy = true
            localError = null
            statusMessage = context.getString(R.string.extracting_colors)
            paletteColors = emptyList()
            markerPositions = emptyList()
            selectedColorIndex = 0
            loupeTouchPosition = null
            loupeSample = null
            imageBitmap = null

            paletteViewModel.generateFromRecentUpload(
                imageUrl = imageUrl,
                colorCount = paletteSize(),
                onDone = { colors ->
                    isBusy = false
                    if (colors.isEmpty()) {
                        localError = context.getString(R.string.extract_colors_failed)
                        return@generateFromRecentUpload
                    }
                    applyPalette(colors, "${context.getString(R.string.extracted_colors_count, colors.size)} ($fileName)")
                },
                onError = { error ->
                    isBusy = false
                    localError = error
                },
            )
        }
    }

    fun recalculateFromCurrentImage() {
        val currentBitmap = imageBitmap ?: return
        scope.launch {
            val bytes = lastImageStorage.read()
            if (bytes == null || bytes.isEmpty()) return@launch
            isBusy = true
            localError = null
            statusMessage = context.getString(R.string.extracting_colors)
            paletteColors = emptyList()
            markerPositions = emptyList()
            selectedColorIndex = 0
            loupeTouchPosition = null
            loupeSample = null
            paletteViewModel.generateFromImage(
                fileName = "last_image.jpg",
                imageBytes = bytes,
                colorCount = paletteSize(),
                onDone = { colors ->
                    isBusy = false
                    if (colors.isEmpty()) {
                        localError = context.getString(R.string.extract_colors_failed)
                        return@generateFromImage
                    }
                    applyPalette(colors, context.getString(R.string.extracted_colors_count, colors.size))
                    scope.launch {
                        val markers = withContext(Dispatchers.Default) {
                            estimateInitialMarkerPositions(currentBitmap, colors)
                        }
                        markerPositions = markers
                    }
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
            statusMessage = context.getString(R.string.export_canceled)
            pendingExport = null
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            writeExportFile(context, outputUri, payload)
                .onSuccess {
                    statusMessage = context.getString(R.string.file_saved, payload.fileName)
                    localError = null
                    pendingExport = null
                }
                .onFailure { error ->
                    localError = error.message ?: context.getString(R.string.export_error_generic)
                    pendingExport = null
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
    val imageAspectRatio = remember(bitmap) {
        bitmap?.let { it.width.toFloat() / it.height.toFloat() }
            ?.coerceIn(0.4f, 1.8f)
            ?: 1f
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
                    title = {
                        Text(
                            text = if (mode == PaletteGenerateScreenMode.RANDOM) {
                                stringResource(id = R.string.generator_random_page)
                            } else {
                                stringResource(id = R.string.generator_image_page)
                            },
                        )
                    },
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
                            title = stringResource(id = R.string.source_title),
                            subtitle = if (mode == PaletteGenerateScreenMode.RANDOM) {
                                stringResource(id = R.string.source_random_subtitle)
                            } else {
                                stringResource(id = R.string.source_image_subtitle)
                            },
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

                    ColorCountDropdown(
                        label = stringResource(id = R.string.color_count_hint),
                        selectedCount = colorCount,
                        onSelected = {
                            if (it != null) {
                                colorCount = it
                                localError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (mode == PaletteGenerateScreenMode.RANDOM) {
                            PaletaPrimaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.generate_random),
                                onClick = {
                                    imageBitmap = null
                                    markerPositions = emptyList()
                                    applyPalette(
                                        colors = RandomPaletteGenerator.generate(paletteSize()),
                                        message = context.getString(R.string.random_palette_generated),
                                    )
                                },
                                enabled = !isBusy,
                            )
                        } else {
                            PaletaPrimaryButton(
                                modifier = if (imageBitmap != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.pick_image),
                                onClick = { pickImageLauncher.launch("image/*") },
                                enabled = !isBusy,
                            )
                            if (imageBitmap != null) {
                                PaletaGhostButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = R.string.recalculate),
                                    onClick = { recalculateFromCurrentImage() },
                                    enabled = !isBusy,
                                )
                            }
                        }
                    }

                    if (mode == PaletteGenerateScreenMode.RANDOM && hasPalette) {
                        ColorHarmonySection(
                            baseHex = selectedHex,
                            colorCount = paletteColors.size.coerceAtLeast(3),
                            onApply = { harmonyColors ->
                                applyPalette(
                                    colors = harmonyColors,
                                    message = context.getString(R.string.harmony_applied),
                                )
                            },
                        )
                    }
                }

                if (mode == PaletteGenerateScreenMode.IMAGE && paletteState.recentUploads.isNotEmpty()) {
                    PaletaCard(modifier = Modifier.fillMaxWidth()) {
                        PaletaSectionTitle(
                            title = stringResource(id = R.string.recent_uploads_title),
                            subtitle = stringResource(id = R.string.recent_uploads_subtitle),
                        )
                        paletteState.recentUploads.take(8).forEach { upload ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = upload.filename,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                PaletaGhostButton(
                                    modifier = Modifier.width(116.dp),
                                    text = stringResource(id = R.string.use_action),
                                    onClick = { useRecentUpload(upload.url, upload.filename) },
                                    enabled = !isBusy,
                                )
                            }
                        }
                    }
                }

                if (mode == PaletteGenerateScreenMode.IMAGE && bitmap != null) {
                    PaletaCard(modifier = Modifier.fillMaxWidth()) {
                        PaletaSectionTitle(
                            title = stringResource(id = R.string.pipette_title),
                            subtitle = stringResource(id = R.string.pipette_subtitle),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(imageAspectRatio)
                                .heightIn(min = 240.dp, max = 520.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .onSizeChanged { imageBoxSize = it }
                                .pointerInput(bitmap, fitMetrics) {
                                    val markerCircleSizePx = 24.dp.toPx()
                                    val markerHitPaddingPx = 16.dp.toPx()
                                    awaitEachGesture {
                                        val metrics = fitMetrics ?: return@awaitEachGesture
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val draggingIndex = findMarkerIndexAtPosition(
                                            position = down.position,
                                            markerPositions = markerPositions,
                                            metrics = metrics,
                                            circleSizePx = markerCircleSizePx,
                                            hitPaddingPx = markerHitPaddingPx,
                                        ) ?: return@awaitEachGesture
                                        val currentSelectedIndex = selectedColorIndex
                                            .coerceIn(0, max(0, paletteColors.lastIndex))
                                        if (draggingIndex != currentSelectedIndex) {
                                            selectedColorIndex = draggingIndex
                                            statusMessage = context.getString(R.string.pipette_selected, draggingIndex + 1)
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
                                        statusMessage = context.getString(R.string.color_updated_from_image, safeSelectedIndex + 1)
                                        loupeTouchPosition = null
                                        loupeSample = null
                                    }
                                },
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(id = R.string.selected_image_desc),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )

                            val metrics = fitMetrics
                            if (metrics != null) {
                                markerPositions.forEachIndexed { index, marker ->
                                    if (marker == null) return@forEachIndexed
                                    val markerX = metrics.left + marker.x * metrics.width
                                    val markerY = metrics.top + marker.y * metrics.height
                                    val markerColor = paletteColors.getOrNull(index)?.let {
                                        ColorTools.hexToColorInt(it)?.let(::Color)
                                    } ?: Color.Gray

                                    val markerSize = if (isDraggingPipette && index == safeSelectedIndex) 26.dp else 24.dp
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    x = markerX.roundToInt() - (markerSize / 2).roundToPx(),
                                                    y = markerY.roundToInt() - (markerSize / 2).roundToPx(),
                                                )
                                            }
                                            .size(markerSize)
                                            .clickable(enabled = false) {
                                                selectedColorIndex = index
                                                statusMessage = context.getString(R.string.pipette_selected, index + 1)
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
                                            val loupeSize = IntSize(
                                                width = 72.dp.roundToPx(),
                                                height = 86.dp.roundToPx(),
                                            )
                                            calculateLoupeOffset(
                                                anchor = touch,
                                                containerSize = imageBoxSize,
                                                loupeSize = loupeSize,
                                                verticalMargin = 72.dp.roundToPx(),
                                                horizontalMargin = 48.dp.roundToPx(),
                                            )
                                        },
                                    bitmap = bitmap,
                                    sample = sample,
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.pipette_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = stringResource(id = R.string.palette_colors_title),
                        subtitle = stringResource(id = R.string.palette_colors_subtitle),
                    )

                    if (!hasPalette) {
                        Text(
                            text = stringResource(id = R.string.generate_first_hint),
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
                                val swatchShape = RoundedCornerShape(12.dp)
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
                                            .background(color, swatchShape)
                                            .innerBorder(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                shape = swatchShape,
                                            ),
                                    )
                                    Text(
                                        text = stringResource(id = R.string.color_label, index + 1),
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
                            title = stringResource(id = R.string.color_editor_title),
                            subtitle = stringResource(id = R.string.selected_color_subtitle, safeSelectedIndex + 1, selectedHex),
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
                                text = stringResource(id = R.string.copy_hex),
                                onClick = {
                                    clipboard.setText(AnnotatedString(selectedHex))
                                    statusMessage = context.getString(R.string.hex_copied, selectedHex)
                                },
                            )
                            PaletaGhostButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.random_color),
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
                        onClick = {
                            if (paletteColors.isEmpty()) {
                                localError = context.getString(R.string.palette_not_generated)
                                return@PaletaPrimaryButton
                            }
                            val colors = HexColors.normalize(paletteColors)
                            if (colors == null) {
                                localError = context.getString(R.string.palette_invalid_hex_count)
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
                }

                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = stringResource(id = R.string.save_title),
                        subtitle = stringResource(id = R.string.save_subtitle),
                    )
                    PaletaPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.save_palette),
                        onClick = {
                            if (paletteColors.isEmpty()) {
                                localError = context.getString(R.string.palette_not_generated)
                                return@PaletaPrimaryButton
                            }
                            if (!isAuthenticated) {
                                localError = context.getString(R.string.login_to_save_palette)
                                return@PaletaPrimaryButton
                            } else {
                                val colors = HexColors.normalize(paletteColors)
                                if (colors == null) {
                                    localError = context.getString(R.string.palette_invalid_hex_count)
                                } else {
                                    paletteViewModel.createPalette(
                                        name = paletteName,
                                        colors = colors,
                                    )
                                    statusMessage = context.getString(R.string.palette_saved)
                                    localError = null
                                }
                            }
                        },
                        enabled = !isBusy,
                    )
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

                PaletaTopBannerHost(
                    error = localError,
                    info = statusMessage,
                )
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
    val circleSize = if (dragging) 26.dp else 24.dp
    Box(
        modifier = Modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = accent,
                shape = CircleShape,
            ),
    )
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
    circleSizePx: Float,
    hitPaddingPx: Float,
): Int? {
    return markerPositions.indices
        .reversed()
        .firstOrNull { index ->
            val marker = markerPositions[index] ?: return@firstOrNull false
            val centerX = metrics.left + marker.x * metrics.width
            val centerY = metrics.top + marker.y * metrics.height
            val radius = circleSizePx / 2f + hitPaddingPx
            val dx = position.x - centerX
            val dy = position.y - centerY
            dx * dx + dy * dy <= radius * radius
        }
}

private fun calculateLoupeOffset(
    anchor: Offset,
    containerSize: IntSize,
    loupeSize: IntSize,
    verticalMargin: Int,
    horizontalMargin: Int,
): IntOffset {
    val loupeWidth = loupeSize.width
    val loupeHeight = loupeSize.height

    val maxX = (containerSize.width - loupeWidth).coerceAtLeast(0)
    val maxY = (containerSize.height - loupeHeight).coerceAtLeast(0)

    val ax = anchor.x.roundToInt()
    val ay = anchor.y.roundToInt()

    // Center horizontally over the touch point
    var x = (ax - loupeWidth / 2).coerceIn(0, maxX)

    // Prefer above the finger; fall back to below
    var y = ay - loupeHeight - verticalMargin
    if (y < 0) {
        y = ay + verticalMargin
    }

    // If still off-screen vertically, try to the side
    if (y > maxY) {
        y = (ay - loupeHeight / 2).coerceIn(0, maxY)
        x = if (ax > containerSize.width / 2) {
            (ax - loupeWidth - horizontalMargin).coerceAtLeast(0)
        } else {
            (ax + horizontalMargin).coerceAtMost(maxX)
        }
    }

    return IntOffset(x = x, y = y)
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

    val w = bitmap.width
    val h = bitmap.height
    val stepX = max(1, w / 160)
    val stepY = max(1, h / 160)
    val samples = ArrayList<SamplePoint>()

    val rowBuffer = IntArray(w)
    var y = 0
    while (y < h) {
        bitmap.getPixels(rowBuffer, 0, w, 0, y, w, 1)
        var x = 0
        while (x < w) {
            val color = rowBuffer[x]
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
        val centerX = w / 2
        val centerY = h / 2
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
