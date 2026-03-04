package ru.diamko.paleta.domain.repository

import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile

interface PaletteRepository {
    suspend fun getPalettes(): List<Palette>
    suspend fun createPalette(name: String, colors: List<String>): Palette
    suspend fun renamePalette(id: Long, name: String): Palette
    suspend fun deletePalette(id: Long)
    suspend fun generateFromImage(fileName: String, imageBytes: ByteArray, colorCount: Int): List<String>
    suspend fun exportPalette(name: String, colors: List<String>, format: String): PaletteExportFile
}
