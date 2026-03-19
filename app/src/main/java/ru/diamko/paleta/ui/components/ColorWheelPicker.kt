/**
 * Модуль: ColorWheelPicker.
 * Назначение: Цветовое колесо HSV: выбор оттенка и насыщенности + слайдер яркости.
 */
package ru.diamko.paleta.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.core.palette.ColorTools
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun ColorWheelPicker(
    colorHex: String,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var wheelSize by remember { mutableStateOf(IntSize.Zero) }

    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var value by remember { mutableStateOf(1f) }

    // Sync HSV from external colorHex changes without recreating MutableState
    // (recreating states breaks pointerInput which holds refs to old objects)
    var prevColorHex by remember { mutableStateOf("") }
    if (prevColorHex != colorHex) {
        prevColorHex = colorHex
        val color = ColorTools.hexToColorInt(colorHex) ?: AndroidColor.BLACK
        val hsv = FloatArray(3).also { AndroidColor.colorToHSV(color, it) }
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val currentOnColorChange by rememberUpdatedState(onColorChange)

    fun emitColor() {
        val nextHex = ColorTools.colorIntToHex(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))
        if (!nextHex.equals(colorHex, ignoreCase = true)) {
            currentOnColorChange(nextHex)
        }
    }

    var wheelBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(wheelSize) {
        if (wheelSize.width > 0 && wheelSize.height > 0) {
            wheelBitmap = withContext(Dispatchers.Default) {
                createHueSaturationWheelBitmap(wheelSize)
            }
        }
    }

    fun updateFromPoint(point: Offset) {
        val size = wheelSize
        if (size.width <= 0 || size.height <= 0) return

        val radius = min(size.width, size.height) / 2f
        if (radius <= 0f) return

        val center = Offset(size.width / 2f, size.height / 2f)
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

        saturation = (distance / radius).coerceIn(0f, 1f)
        hue = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f)
        emitColor()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .onSizeChanged { wheelSize = it }
                .pointerInput(wheelSize) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val size = wheelSize
                        if (size.width <= 0 || size.height <= 0) return@awaitEachGesture
                        val radius = min(size.width, size.height) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = down.position.x - center.x
                        val dy = down.position.y - center.y
                        if (sqrt(dx * dx + dy * dy) > radius) return@awaitEachGesture
                        down.consume()
                        updateFromPoint(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            updateFromPoint(change.position)
                        }
                    }
                },
        ) {
            val bitmap = wheelBitmap ?: return@Canvas
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )

            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = radius + 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            val angleRad = Math.toRadians(hue.toDouble())
            val markerX = center.x + (saturation * radius * kotlin.math.cos(angleRad).toFloat())
            val markerY = center.y + (saturation * radius * kotlin.math.sin(angleRad).toFloat())
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(markerX, markerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Text(
            text = stringResource(id = R.string.color_wheel_brightness, (value * 100f).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = {
                value = it.coerceIn(0f, 1f)
                emitColor()
            },
            valueRange = 0f..1f,
        )
    }
}

private fun createHueSaturationWheelBitmap(size: IntSize): Bitmap? {
    if (size.width <= 0 || size.height <= 0) return null
    val width = size.width
    val height = size.height
    val radius = min(width, height) / 2f
    if (radius <= 0f) return null

    val pixels = IntArray(width * height)
    val centerX = width / 2f
    val centerY = height / 2f
    val hsv = FloatArray(3)
    hsv[2] = 1f

    for (y in 0 until height) {
        val dy = y - centerY
        val offset = y * width
        for (x in 0 until width) {
            val dx = x - centerX
            val distance = sqrt(dx * dx + dy * dy)
            if (distance > radius) {
                pixels[offset + x] = AndroidColor.TRANSPARENT
                continue
            }
            hsv[0] = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
            hsv[1] = (distance / radius).coerceIn(0f, 1f)
            pixels[offset + x] = AndroidColor.HSVToColor(hsv)
        }
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
