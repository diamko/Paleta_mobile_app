package ru.diamko.paleta.domain.repository

import ru.diamko.paleta.domain.model.Palette

interface PaletteRepository {
    suspend fun getPalettes(): List<Palette>
    suspend fun createPalette(name: String, colors: List<String>): Palette
    suspend fun renamePalette(id: Long, name: String): Palette
    suspend fun deletePalette(id: Long)
}
