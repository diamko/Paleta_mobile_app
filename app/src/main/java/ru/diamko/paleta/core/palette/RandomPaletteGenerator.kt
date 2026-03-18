/**
 * Модуль: RandomPaletteGenerator.
 * Назначение: Генератор случайных цветовых палитр с учётом гармонии.
 */
package ru.diamko.paleta.core.palette

import android.graphics.Color
import kotlin.random.Random

object RandomPaletteGenerator {
    fun generate(colorCount: Int): List<String> {
        val count = colorCount.coerceIn(3, 15)
        val baseHue = Random.nextFloat() * 360f
        val hueStep = 360f / count.toFloat()

        return buildList {
            repeat(count) { index ->
                val hueJitter = Random.nextFloat() * 16f - 8f
                val sat = (0.55f + Random.nextFloat() * 0.35f).coerceIn(0f, 1f)
                val value = (0.6f + Random.nextFloat() * 0.35f).coerceIn(0f, 1f)
                val hue = ((baseHue + hueStep * index + hueJitter) % 360f + 360f) % 360f
                val colorInt = Color.HSVToColor(floatArrayOf(hue, sat, value))
                add(ColorTools.colorIntToHex(colorInt))
            }
        }.distinct().ifEmpty {
            listOf(
                "#1F77B4",
                "#FF7F0E",
                "#2CA02C",
            )
        }
    }

}
