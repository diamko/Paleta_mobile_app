/**
 * Модуль: ColorHarmony.
 * Назначение: Генерация гармоничных цветов: аналоговые, триадные, комплементарные и др.
 */
package ru.diamko.paleta.core.palette

import android.graphics.Color

enum class ColorHarmonyType(val patternSize: Int?) {
    SEQUENTIAL(null),
    ANALOGOUS(null),
    MONOCHROMATIC(null),
    TRIADIC(3),
    COMPLEMENTARY(4),
    SQUARE(4),
    SPLIT_COMPLEMENTARY(3),
    TETRADIC(4),
    ;

    fun isCompatibleWith(colorCount: Int): Boolean {
        val ps = patternSize ?: return true
        return colorCount % ps == 0
    }
}

object ColorHarmony {
    fun generate(
        baseHex: String,
        type: ColorHarmonyType,
        count: Int,
    ): List<String> {
        val base = ColorTools.hexToColorInt(baseHex) ?: return emptyList()
        val safeCount = count.coerceIn(3, 15)
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        val baseHue = hsv[0]
        val baseSat = hsv[1]
        val baseValue = hsv[2]

        return when (type) {
            ColorHarmonyType.SEQUENTIAL -> sequential(baseHue, baseSat, baseValue, safeCount)
            ColorHarmonyType.ANALOGOUS -> offsets(baseHue, baseSat, baseValue, safeCount, listOf(-30f, -15f, 0f, 15f, 30f))
            ColorHarmonyType.MONOCHROMATIC -> monochromatic(baseHue, baseSat, baseValue, safeCount)
            ColorHarmonyType.TRIADIC -> offsets(baseHue, baseSat, baseValue, safeCount, listOf(0f, 120f, 240f))
            ColorHarmonyType.COMPLEMENTARY -> offsets(baseHue, baseSat, baseValue, safeCount, listOf(0f, 180f, 24f, 204f))
            ColorHarmonyType.SQUARE -> offsets(baseHue, baseSat, baseValue, safeCount, listOf(0f, 90f, 180f, 270f))
            ColorHarmonyType.SPLIT_COMPLEMENTARY -> offsets(baseHue, baseSat, baseValue, safeCount, listOf(0f, 150f, 210f))
            ColorHarmonyType.TETRADIC -> offsets(baseHue, baseSat, baseValue, safeCount, listOf(0f, 60f, 180f, 240f))
        }
    }

    private fun sequential(
        hue: Float,
        sat: Float,
        value: Float,
        count: Int,
    ): List<String> {
        if (count <= 1) return listOf(toHex(hue, sat, value))
        val step = 220f / (count - 1).toFloat()
        return List(count) { index ->
            val currentHue = normalizeHue(hue - 110f + step * index)
            val satShift = ((index - count / 2f) * 0.02f)
            val valueShift = ((count / 2f - index) * 0.02f)
            toHex(
                hue = currentHue,
                sat = (sat + satShift).coerceIn(0.2f, 1f),
                value = (value + valueShift).coerceIn(0.25f, 1f),
            )
        }
    }

    private fun offsets(
        hue: Float,
        sat: Float,
        value: Float,
        count: Int,
        offsetPattern: List<Float>,
    ): List<String> {
        return List(count) { index ->
            val offset = offsetPattern[index % offsetPattern.size]
            val cycle = index / offsetPattern.size
            val satShift = if (cycle % 2 == 0) 0f else 0.08f
            val valueShift = when (cycle % 3) {
                0 -> 0f
                1 -> -0.12f
                else -> 0.12f
            }
            toHex(
                hue = normalizeHue(hue + offset),
                sat = (sat + satShift).coerceIn(0.2f, 1f),
                value = (value + valueShift).coerceIn(0.2f, 1f),
            )
        }
    }

    private fun monochromatic(
        hue: Float,
        sat: Float,
        value: Float,
        count: Int,
    ): List<String> {
        if (count <= 1) return listOf(toHex(hue, sat, value))
        return List(count) { index ->
            val t = index.toFloat() / (count - 1).toFloat()
            val targetSat = lerp(0.15f, 0.95f, t)
            val targetVal = lerp(0.25f, 0.95f, 1f - t)
            toHex(
                hue = hue,
                sat = ((targetSat + sat) / 2f).coerceIn(0f, 1f),
                value = ((targetVal + value) / 2f).coerceIn(0f, 1f),
            )
        }
    }

    private fun toHex(hue: Float, sat: Float, value: Float): String {
        val colorInt = Color.HSVToColor(floatArrayOf(normalizeHue(hue), sat, value))
        return ColorTools.colorIntToHex(colorInt)
    }

    private fun normalizeHue(hue: Float): Float {
        return ((hue % 360f) + 360f) % 360f
    }

    private fun lerp(start: Float, end: Float, t: Float): Float {
        return (start + (end - start) * t).coerceIn(start.coerceAtMost(end), start.coerceAtLeast(end))
    }
}
