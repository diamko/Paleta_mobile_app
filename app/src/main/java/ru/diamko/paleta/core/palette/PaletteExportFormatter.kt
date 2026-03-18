/**
 * Модуль: PaletteExportFormatter.
 * Назначение: Форматирование палитры для экспорта в CSS, JSON, SVG и др.
 */
package ru.diamko.paleta.core.palette

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

enum class PaletteExportFormat(
    val ext: String,
    val mimeType: String,
) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    GPL("gpl", "text/plain"),
    ASE("ase", "application/octet-stream"),
    ACO("aco", "application/octet-stream"),
    PNG("png", "image/png"),
}

data class PaletteExportPayload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

object PaletteExportFormatter {
    fun format(
        name: String,
        colors: List<String>,
        format: PaletteExportFormat,
    ): PaletteExportPayload {
        val safeName = sanitizeFileName(name.ifBlank { "palette" })
        return when (format) {
            PaletteExportFormat.JSON -> {
                val json = buildJson(colors)
                PaletteExportPayload("$safeName.json", format.mimeType, json.toByteArray(StandardCharsets.UTF_8))
            }

            PaletteExportFormat.CSV -> {
                val csv = buildCsv(colors)
                PaletteExportPayload("$safeName.csv", format.mimeType, csv.toByteArray(StandardCharsets.UTF_8))
            }

            PaletteExportFormat.GPL -> {
                val gpl = buildGpl(name.ifBlank { "Palette" }, colors)
                PaletteExportPayload("$safeName.gpl", format.mimeType, gpl.toByteArray(StandardCharsets.UTF_8))
            }

            PaletteExportFormat.ASE -> {
                val ase = buildAse(name.ifBlank { "Palette" }, colors)
                PaletteExportPayload("$safeName.ase", format.mimeType, ase)
            }

            PaletteExportFormat.ACO -> {
                val aco = buildAco(colors)
                PaletteExportPayload("$safeName.aco", format.mimeType, aco)
            }

            PaletteExportFormat.PNG -> {
                val png = buildPng(colors)
                PaletteExportPayload("$safeName.png", format.mimeType, png)
            }
        }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.trim().replace(Regex("[^\\p{L}\\p{N}_\\-. ]"), "_").ifBlank { "palette" }
    }

    private fun buildJson(colors: List<String>): String {
        val items = colors.joinToString(",\n") { "  \"$it\"" }
        return "{\n\"colors\": [\n$items\n]\n}\n"
    }

    private fun buildCsv(colors: List<String>): String {
        val rows = colors.joinToString("\n") { hex ->
            val c = ColorTools.hexToColorInt(hex) ?: Color.BLACK
            "$hex,${Color.red(c)},${Color.green(c)},${Color.blue(c)}"
        }
        return "hex,r,g,b\n$rows\n"
    }

    private fun buildGpl(name: String, colors: List<String>): String {
        val lines = colors.joinToString("\n") { hex ->
            val c = ColorTools.hexToColorInt(hex) ?: Color.BLACK
            "${Color.red(c).toString().padStart(3)} ${Color.green(c).toString().padStart(3)} " +
                "${Color.blue(c).toString().padStart(3)} $hex"
        }
        return buildString {
            appendLine("GIMP Palette")
            appendLine("Name: $name")
            appendLine("Columns: ${colors.size.coerceAtMost(10)}")
            appendLine("#")
            appendLine(lines)
        }
    }

    private fun buildAco(colors: List<String>): ByteArray {
        val buffer = ByteBuffer.allocate(4 + colors.size * 10)
            .order(ByteOrder.BIG_ENDIAN)
        buffer.putShort(1) // version
        buffer.putShort(colors.size.toShort())
        colors.forEach { hex ->
            val c = ColorTools.hexToColorInt(hex) ?: Color.BLACK
            buffer.putShort(0) // RGB
            buffer.putShort((Color.red(c) * 257).toShort())
            buffer.putShort((Color.green(c) * 257).toShort())
            buffer.putShort((Color.blue(c) * 257).toShort())
            buffer.putShort(0)
        }
        return buffer.array()
    }

    private fun buildAse(name: String, colors: List<String>): ByteArray {
        val blocks = colors.mapIndexed { index, hex ->
            val blockName = "$name ${index + 1}"
            buildAseColorBlock(blockName, hex)
        }
        val header = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN).apply {
            put("ASEF".toByteArray(StandardCharsets.US_ASCII))
            putShort(1)
            putShort(0)
            putInt(blocks.size)
        }.array()

        val output = ByteArrayOutputStream()
        output.write(header)
        blocks.forEach { output.write(it) }
        return output.toByteArray()
    }

    private fun buildAseColorBlock(name: String, hex: String): ByteArray {
        val c = ColorTools.hexToColorInt(hex) ?: Color.BLACK
        val utf16Name = (name + "\u0000").toByteArray(StandardCharsets.UTF_16BE)
        val nameCharCount = (utf16Name.size / 2).toShort()

        val body = ByteBuffer.allocate(2 + utf16Name.size + 4 + 12 + 2).order(ByteOrder.BIG_ENDIAN).apply {
            putShort(nameCharCount)
            put(utf16Name)
            put("RGB ".toByteArray(StandardCharsets.US_ASCII))
            putFloat(Color.red(c) / 255f)
            putFloat(Color.green(c) / 255f)
            putFloat(Color.blue(c) / 255f)
            putShort(0) // global color
        }.array()

        val blockHeader = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN).apply {
            putShort(0x0001) // color entry
            putInt(body.size)
        }.array()

        return blockHeader + body
    }

    private fun buildPng(colors: List<String>): ByteArray {
        val width = maxOf(480, colors.size * 180)
        val height = 260
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val swatchWidth = width / colors.size.toFloat()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }
        colors.forEachIndexed { index, hex ->
            val c = ColorTools.hexToColorInt(hex) ?: Color.BLACK
            val left = index * swatchWidth
            val right = left + swatchWidth
            val paint = Paint().apply { color = c }
            canvas.drawRect(left, 0f, right, (height - 56).toFloat(), paint)
            canvas.drawText(hex, left + 16f, (height - 18).toFloat(), textPaint)
        }

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }
}
