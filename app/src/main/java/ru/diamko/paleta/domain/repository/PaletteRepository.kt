/**
 * Модуль: PaletteRepository.
 * Назначение: Интерфейс репозитория палитр.
 */
package ru.diamko.paleta.domain.repository

import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.domain.model.RecentUpload

interface PaletteRepository {
    suspend fun getPalettes(): List<Palette>
    suspend fun getRecentUploads(days: Int = 7): List<RecentUpload>
    suspend fun createPalette(name: String, colors: List<String>): Palette
    suspend fun renamePalette(id: Long, name: String): Palette
    suspend fun deletePalette(id: Long)
    suspend fun generateFromImage(fileName: String, imageBytes: ByteArray, colorCount: Int): List<String>
    suspend fun generateFromImageUrl(imageUrl: String, colorCount: Int): List<String>
    suspend fun exportPalette(name: String, colors: List<String>, format: String): PaletteExportFile
}
