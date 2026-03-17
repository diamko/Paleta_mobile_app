package ru.diamko.paleta.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp

fun Modifier.innerBorder(
    width: Dp,
    color: Color,
    shape: Shape,
): Modifier {
    return drawWithContent {
        drawContent()
        val strokePx = width.toPx()
        if (strokePx <= 0f) return@drawWithContent
        inset(strokePx / 2f, strokePx / 2f) {
            val outline = shape.createOutline(size, layoutDirection, this)
            when (outline) {
                is Outline.Rectangle -> {
                    drawRect(color = color, style = Stroke(width = strokePx))
                }
                is Outline.Rounded -> {
                    val roundRect = outline.roundRect
                    val path = Path().apply { addRoundRect(roundRect) }
                    drawPath(path = path, color = color, style = Stroke(width = strokePx))
                }
                is Outline.Generic -> {
                    drawPath(path = outline.path, color = color, style = Stroke(width = strokePx))
                }
            }
        }
    }
}
