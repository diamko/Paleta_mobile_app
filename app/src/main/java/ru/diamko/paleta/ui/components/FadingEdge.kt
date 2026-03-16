package ru.diamko.paleta.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun Modifier.fadingEdge(scrollState: ScrollState): Modifier {
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadeWidth = 24.dp.toPx()
            if (scrollState.value > 0) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = 0f,
                        endX = fadeWidth,
                    ),
                    size = Size(fadeWidth, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = size.width - fadeWidth,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - fadeWidth, 0f),
                    size = Size(fadeWidth, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}
