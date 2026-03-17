package ru.diamko.paleta.ui.palettes

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import ru.diamko.paleta.core.palette.ColorHarmony
import ru.diamko.paleta.core.palette.ColorHarmonyType
import ru.diamko.paleta.core.palette.ColorTools
import ru.diamko.paleta.ui.components.HorizontalScrollIndicator
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaSectionTitle

@Composable
fun ColorHarmonySection(
    baseHex: String,
    colorCount: Int,
    onApply: (List<String>) -> Unit,
    onColorsGenerated: (List<String>) -> Unit = {},
) {
    var selectedType by remember { mutableStateOf(ColorHarmonyType.ANALOGOUS) }
    val generated = remember(baseHex, colorCount, selectedType) {
        ColorHarmony.generate(
            baseHex = baseHex,
            type = selectedType,
            count = colorCount,
        )
    }

    LaunchedEffect(generated) {
        onColorsGenerated(generated)
    }

    PaletaSectionTitle(
        title = stringResource(id = R.string.harmony_title),
        subtitle = stringResource(id = R.string.harmony_subtitle),
    )

    val harmonyScrollState = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(harmonyScrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorHarmonyType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { selectedType = type },
                label = { Text(text = stringResource(id = type.labelResId())) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
    HorizontalScrollIndicator(scrollState = harmonyScrollState)

    if (generated.isNotEmpty()) {
        val colorsScrollState = rememberScrollState()
        Row(
            modifier = Modifier.horizontalScroll(colorsScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            generated.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = ColorTools.hexToColorInt(hex)?.let(::Color) ?: Color.Gray,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(8.dp),
                        ),
                )
            }
        }
    }

    PaletaGhostButton(
        text = stringResource(id = R.string.harmony_apply),
        onClick = { onApply(generated) },
        enabled = generated.isNotEmpty(),
    )
}

private fun ColorHarmonyType.labelResId(): Int {
    return when (this) {
        ColorHarmonyType.SEQUENTIAL -> R.string.harmony_sequential
        ColorHarmonyType.ANALOGOUS -> R.string.harmony_analogous
        ColorHarmonyType.MONOCHROMATIC -> R.string.harmony_monochromatic
        ColorHarmonyType.TRIADIC -> R.string.harmony_triadic
        ColorHarmonyType.COMPLEMENTARY -> R.string.harmony_complementary
        ColorHarmonyType.SQUARE -> R.string.harmony_square
        ColorHarmonyType.SPLIT_COMPLEMENTARY -> R.string.harmony_split_complementary
        ColorHarmonyType.TETRADIC -> R.string.harmony_tetradic
    }
}
