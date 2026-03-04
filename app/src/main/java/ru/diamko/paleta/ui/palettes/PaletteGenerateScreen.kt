package ru.diamko.paleta.ui.palettes

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.core.palette.HexColors
import ru.diamko.paleta.core.palette.PaletteExportFormat
import ru.diamko.paleta.core.palette.RandomPaletteGenerator
import ru.diamko.paleta.domain.model.PaletteExportFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteGenerateScreen(
    paletteViewModel: PaletteViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var paletteName by remember { mutableStateOf("Моя палитра") }
    var colorCountRaw by remember { mutableStateOf("5") }
    var colorsInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(PaletteExportFormat.JSON) }
    var pendingExport by remember { mutableStateOf<PaletteExportFile?>(null) }

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

    fun paletteSize(): Int {
        return colorCountRaw.toIntOrNull()?.coerceIn(3, 15) ?: 5
    }

    fun extractFromImage(uri: Uri) {
        scope.launch {
            isBusy = true
            localError = null
            statusMessage = null
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) {
                isBusy = false
                localError = "Не удалось прочитать изображение"
                return@launch
            }
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
                    colorsInput = colors.joinToString(",")
                    statusMessage = "Извлечено цветов: ${colors.size}"
                    localError = null
                },
                onError = { error ->
                    isBusy = false
                    localError = error
                },
            )
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedImageUri = uri
        extractFromImage(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.generate_palette_title)) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = paletteName,
                onValueChange = {
                    paletteName = it
                    localError = null
                },
                label = { Text(stringResource(id = R.string.palette_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val colors = RandomPaletteGenerator.generate(paletteSize())
                        colorsInput = colors.joinToString(",")
                        localError = null
                        statusMessage = "Сгенерирована случайная палитра"
                    },
                ) {
                    Text(stringResource(id = R.string.generate_random))
                }

                Button(onClick = { pickImageLauncher.launch("image/*") }) {
                    Text(stringResource(id = R.string.pick_image))
                }
            }

            selectedImageUri?.let { imageUri ->
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    factory = { viewContext ->
                        ImageView(viewContext).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = { imageView ->
                        imageView.setImageURI(imageUri)
                    },
                )
            }

            OutlinedTextField(
                value = colorsInput,
                onValueChange = {
                    colorsInput = it
                    localError = null
                    statusMessage = null
                },
                label = { Text(stringResource(id = R.string.palette_colors_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )

            val parsedColors = remember(colorsInput) { HexColors.parse(colorsInput) }
            if (!parsedColors.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    parsedColors.forEach { hex ->
                        val color = ColorTools.hexToColorInt(hex)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (color != null) {
                                            Color(color)
                                        } else {
                                            Color.Gray
                                        },
                                    ),
                            )
                            Text(text = hex, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaletteExportFormat.entries.forEach { format ->
                    TextButton(
                        onClick = { selectedFormat = format },
                    ) {
                        val label = if (format == selectedFormat) {
                            "[${format.name}]"
                        } else {
                            format.name
                        }
                        Text(label)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val colors = HexColors.parse(colorsInput)
                        if (colors == null) {
                            localError = "Введите от 3 до 15 корректных HEX-цветов"
                            return@Button
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
                ) {
                    Text(stringResource(id = R.string.export_file))
                }

                Button(
                    onClick = {
                        val colors = HexColors.parse(colorsInput)
                        if (colors == null) {
                            localError = "Введите от 3 до 15 корректных HEX-цветов"
                            return@Button
                        }
                        paletteViewModel.createPalette(
                            name = paletteName,
                            colors = colors,
                        )
                        statusMessage = "Палитра сохранена"
                        localError = null
                    },
                ) {
                    Text(stringResource(id = R.string.save_palette))
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

            localError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            statusMessage?.let { message ->
                Text(text = message)
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) {
                Text(stringResource(id = R.string.back))
            }
        }
    }
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String {
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
