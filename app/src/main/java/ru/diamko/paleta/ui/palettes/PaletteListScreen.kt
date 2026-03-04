package ru.diamko.paleta.ui.palettes

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaMessageBanner
import ru.diamko.paleta.ui.components.PaletaPrimaryButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle
import ru.diamko.paleta.ui.components.paletaTextFieldColors

private enum class PaletteSortMode {
    NEWEST,
    OLDEST,
    NAME,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteListScreen(
    state: PaletteUiState,
    onReload: () -> Unit,
    onCreateClick: () -> Unit,
    onOpenGenerator: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(PaletteSortMode.NEWEST.name) }

    LaunchedEffect(Unit) {
        onReload()
    }

    val visiblePalettes = remember(state.palettes, query, sortMode) {
        val filtered = if (query.isBlank()) {
            state.palettes
        } else {
            val q = query.trim().lowercase()
            state.palettes.filter { palette ->
                palette.name.lowercase().contains(q) ||
                    palette.colors.any { it.lowercase().contains(q) }
            }
        }

        when (PaletteSortMode.valueOf(sortMode)) {
            PaletteSortMode.NEWEST -> filtered.sortedByDescending { it.createdAtIso }
            PaletteSortMode.OLDEST -> filtered.sortedBy { it.createdAtIso }
            PaletteSortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
        }
    }
    val listError = state.error

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
                        Text(
                            text = stringResource(id = R.string.palettes_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PaletaCard(modifier = Modifier.fillMaxWidth()) {
                        PaletaSectionTitle(
                            title = "Paleta Mobile",
                            subtitle = "Создавайте, генерируйте и экспортируйте палитры прямо с телефона",
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PaletaPrimaryButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.generate_palette_title),
                                onClick = onOpenGenerator,
                            )
                            PaletaGhostButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.new_palette),
                                onClick = onCreateClick,
                            )
                        }
                    }
                }

                if (listError != null) {
                    item {
                        PaletaMessageBanner(
                            message = listError,
                            isError = true,
                        )
                    }
                }

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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SortChip(
                            title = stringResource(id = R.string.sort_newest),
                            selected = sortMode == PaletteSortMode.NEWEST.name,
                            onClick = { sortMode = PaletteSortMode.NEWEST.name },
                        )
                        SortChip(
                            title = stringResource(id = R.string.sort_oldest),
                            selected = sortMode == PaletteSortMode.OLDEST.name,
                            onClick = { sortMode = PaletteSortMode.OLDEST.name },
                        )
                        SortChip(
                            title = stringResource(id = R.string.sort_name),
                            selected = sortMode == PaletteSortMode.NAME.name,
                            onClick = { sortMode = PaletteSortMode.NAME.name },
                        )
                    }
                }

                if (visiblePalettes.isEmpty()) {
                    item {
                        PaletaCard(modifier = Modifier.fillMaxWidth()) {
                            PaletaSectionTitle(
                                title = stringResource(id = R.string.empty_palettes),
                                subtitle = "Создайте первую палитру вручную или сгенерируйте из изображения",
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PaletaPrimaryButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = R.string.new_palette),
                                    onClick = onCreateClick,
                                )
                                PaletaGhostButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = R.string.generate_palette_title),
                                    onClick = onOpenGenerator,
                                )
                            }
                        }
                    }
                } else {
                    items(visiblePalettes, key = { it.id }) { palette ->
                        PaletteCard(
                            palette = palette,
                            onEditClick = onEditClick,
                            onDeleteClick = onDeleteClick,
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.height(14.dp))
                }
            }
        }
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
private fun PaletteCard(
    palette: Palette,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
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
            text = "Создано: ${palette.createdAtIso.replace("T", " ").take(16)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palette.colors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)),
                )
            }
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
