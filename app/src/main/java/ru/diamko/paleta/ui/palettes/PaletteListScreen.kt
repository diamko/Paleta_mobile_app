package ru.diamko.paleta.ui.palettes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.PaletteExportFormat
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.ui.components.HorizontalScrollIndicator
import ru.diamko.paleta.ui.components.ColorCountDropdown
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.PaletaTopBannerHost
import ru.diamko.paleta.ui.components.paletaTextFieldColors
import ru.diamko.paleta.ui.theme.BrandBlue
import ru.diamko.paleta.ui.theme.BrandCoral
import ru.diamko.paleta.ui.theme.BrandSuccess
import ru.diamko.paleta.ui.theme.BrandViolet

private enum class PaletteSortMode {
    NEWEST,
    OLDEST,
    NAME_ASC,
    NAME_DESC,
    COLORS_ASC,
    COLORS_DESC,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteListScreen(
    state: PaletteUiState,
    onReload: () -> Unit,
    onCreateClick: () -> Unit,
    onOpenRandomGenerator: () -> Unit,
    onOpenImageGenerator: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    isAuthenticated: Boolean,
    onExportPalette: (
        palette: Palette,
        format: String,
        onDone: (PaletteExportFile) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(PaletteSortMode.NEWEST.name) }
    var colorCountFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedFormat by rememberSaveable { mutableStateOf(PaletteExportFormat.JSON.name) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<Palette?>(null) }
    var pendingExport by remember { mutableStateOf<PaletteExportFile?>(null) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onReload()
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

    val visiblePalettes = remember(state.palettes, query, sortMode, colorCountFilter) {
        val filteredByQuery = if (query.isBlank()) {
            state.palettes
        } else {
            val q = query.trim().lowercase()
            state.palettes.filter { palette ->
                palette.name.lowercase().contains(q) ||
                    palette.colors.any { it.lowercase().contains(q) }
            }
        }
        val filtered = if (colorCountFilter == null) {
            filteredByQuery
        } else {
            filteredByQuery.filter { it.colors.size == colorCountFilter }
        }
        when (PaletteSortMode.valueOf(sortMode)) {
            PaletteSortMode.NEWEST -> filtered.sortedByDescending { it.createdAtIso }
            PaletteSortMode.OLDEST -> filtered.sortedBy { it.createdAtIso }
            PaletteSortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            PaletteSortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            PaletteSortMode.COLORS_ASC -> filtered.sortedWith(compareBy<Palette> { it.colors.size }.thenByDescending { it.createdAtIso })
            PaletteSortMode.COLORS_DESC -> filtered.sortedWith(compareByDescending<Palette> { it.colors.size }.thenByDescending { it.createdAtIso })
        }
    }
    val listError = state.error ?: localError
    val exportFormat = remember(selectedFormat) { PaletteExportFormat.valueOf(selectedFormat) }

    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.paleta_logo),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.palettes_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    actions = {
                        PaletaGhostButton(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .width(116.dp),
                            text = stringResource(id = R.string.settings),
                            onClick = onOpenSettings,
                        )
                    },
                )
            },
        ) { paddingValues ->
            if (state.isLoading && state.palettes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(text = stringResource(id = R.string.loading))
                    }
                }
                return@Scaffold
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.isOffline) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.offline_banner),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    item {
                        PaletaCard(modifier = Modifier.fillMaxWidth()) {
                            PaletaSectionTitle(
                                title = stringResource(id = R.string.home_title),
                                subtitle = stringResource(id = R.string.home_subtitle),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PaletaHomeActionButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = R.string.generator_random_page),
                                    containerColor = BrandViolet.copy(alpha = 0.10f),
                                    borderBrush = Brush.horizontalGradient(
                                        listOf(
                                            BrandViolet.copy(alpha = 0.70f),
                                            BrandBlue.copy(alpha = 0.70f),
                                        ),
                                    ),
                                    onClick = onOpenRandomGenerator,
                                )
                                PaletaHomeActionButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = R.string.generator_image_page),
                                    containerColor = BrandBlue.copy(alpha = 0.10f),
                                    borderBrush = Brush.horizontalGradient(
                                        listOf(
                                            BrandBlue.copy(alpha = 0.70f),
                                            BrandCoral.copy(alpha = 0.70f),
                                        ),
                                    ),
                                    onClick = onOpenImageGenerator,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PaletaHomeActionButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = R.string.new_palette),
                                    containerColor = BrandSuccess.copy(alpha = 0.10f),
                                    borderBrush = Brush.horizontalGradient(
                                        listOf(
                                            BrandSuccess.copy(alpha = 0.70f),
                                            BrandViolet.copy(alpha = 0.70f),
                                        ),
                                    ),
                                    onClick = onCreateClick,
                                )
                            }
                        }
                    }

                    if (isAuthenticated) {
                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                label = { Text(stringResource(id = R.string.search_palettes)) },
                                colors = paletaTextFieldColors(),
                            )
                        }

                        item {
                            ColorCountDropdown(
                                label = stringResource(id = R.string.filter_exact_colors),
                                selectedCount = colorCountFilter,
                                onSelected = { colorCountFilter = it },
                                allowAny = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SortChip(stringResource(id = R.string.sort_newest), sortMode == PaletteSortMode.NEWEST.name) { sortMode = PaletteSortMode.NEWEST.name }
                                SortChip(stringResource(id = R.string.sort_oldest), sortMode == PaletteSortMode.OLDEST.name) { sortMode = PaletteSortMode.OLDEST.name }
                                SortChip(stringResource(id = R.string.sort_name), sortMode == PaletteSortMode.NAME_ASC.name) { sortMode = PaletteSortMode.NAME_ASC.name }
                                SortChip(stringResource(id = R.string.sort_name_desc), sortMode == PaletteSortMode.NAME_DESC.name) { sortMode = PaletteSortMode.NAME_DESC.name }
                                SortChip(stringResource(id = R.string.sort_colors_asc), sortMode == PaletteSortMode.COLORS_ASC.name) { sortMode = PaletteSortMode.COLORS_ASC.name }
                                SortChip(stringResource(id = R.string.sort_colors_desc), sortMode == PaletteSortMode.COLORS_DESC.name) { sortMode = PaletteSortMode.COLORS_DESC.name }
                            }
                        }

                        item {
                            val exportScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier.horizontalScroll(exportScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PaletteExportFormat.entries.forEach { format ->
                                    SortChip(
                                        title = format.name,
                                        selected = selectedFormat == format.name,
                                        onClick = { selectedFormat = format.name },
                                    )
                                }
                            }
                            HorizontalScrollIndicator(scrollState = exportScrollState)
                        }

                        if (visiblePalettes.isEmpty()) {
                            item {
                                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                                    PaletaSectionTitle(
                                        title = stringResource(id = R.string.empty_palettes),
                                        subtitle = stringResource(id = R.string.empty_palettes_subtitle),
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        PaletaGhostButton(
                                            modifier = Modifier.weight(1f),
                                            text = stringResource(id = R.string.generator_random_page),
                                            onClick = onOpenRandomGenerator,
                                        )
                                        PaletaGhostButton(
                                            modifier = Modifier.weight(1f),
                                            text = stringResource(id = R.string.generator_image_page),
                                            onClick = onOpenImageGenerator,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        PaletaGhostButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = stringResource(id = R.string.new_palette),
                                            onClick = onCreateClick,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(visiblePalettes, key = { it.id }) { palette ->
                                PaletteCard(
                                    palette = palette,
                                    onEditClick = onEditClick,
                                    onDeleteClick = { pendingDelete = palette },
                                    onCopyClick = {
                                        clipboard.setText(AnnotatedString(palette.colors.joinToString("\n")))
                                        statusMessage = context.getString(R.string.copy_palette_success)
                                        localError = null
                                    },
                                    onExportClick = {
                                        onExportPalette(
                                            palette,
                                            exportFormat.ext,
                                            { payload ->
                                                pendingExport = payload
                                                createDocumentLauncher.launch(payload.fileName)
                                            },
                                            { error ->
                                                localError = error
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }

                    item {
                        Box(modifier = Modifier.height(14.dp))
                    }
                }

                PaletaTopBannerHost(
                    error = listError,
                    info = statusMessage,
                )
            }
        }
    }

    if (pendingDelete != null) {
        val deleting = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(id = R.string.delete_palette_confirm_title)) },
            text = { Text(stringResource(id = R.string.delete_palette_confirm_message, deleting.name)) },
            confirmButton = {
                PaletaPrimaryButton(
                    text = stringResource(id = R.string.delete),
                    onClick = {
                        onDeleteClick(deleting.id)
                        pendingDelete = null
                    },
                )
            },
            dismissButton = {
                PaletaGhostButton(
                    text = stringResource(id = R.string.cancel),
                    onClick = { pendingDelete = null },
                )
            },
        )
    }
}

@Composable
private fun SortChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = title) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        ),
    )
}

@Composable
private fun PaletaHomeActionButton(
    text: String,
    containerColor: Color,
    borderBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(width = 1.5.dp, brush = borderBrush),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PaletteCard(
    palette: Palette,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onCopyClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    PaletaCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = palette.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = stringResource(id = R.string.created_at, palette.createdAtIso.replace("T", " ").take(16)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val parsedColors = remember(palette.colors) {
            palette.colors.map { hex ->
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            parsedColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaletaGhostButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.copy_action),
                onClick = onCopyClick,
            )
            PaletaGhostButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.export_action),
                onClick = onExportClick,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaletaGhostButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.edit),
                onClick = { onEditClick(palette.id) },
            )
            PaletaGhostButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.delete),
                onClick = { onDeleteClick(palette.id) },
                isDanger = true,
            )
        }
    }
}
