package ru.diamko.paleta.ui.palettes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.domain.model.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteListScreen(
    state: PaletteUiState,
    onReload: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onReload()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.palettes_title)) },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(id = R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = onCreateClick) {
                Text(text = stringResource(id = R.string.new_palette))
            }
        },
    ) { paddingValues ->
        if (state.isLoading && state.palettes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.palettes.isEmpty()) {
                Text(text = stringResource(id = R.string.empty_palettes))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.palettes, key = { it.id }) { palette ->
                        PaletteCard(
                            palette = palette,
                            onEditClick = onEditClick,
                            onDeleteClick = onDeleteClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: Palette,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = palette.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                palette.colors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onEditClick(palette.id) }) {
                    Text(stringResource(id = R.string.edit))
                }
                TextButton(onClick = { onDeleteClick(palette.id) }) {
                    Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
