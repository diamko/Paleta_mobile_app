package ru.diamko.paleta.data.repository

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.palette.BitmapPaletteExtractor
import ru.diamko.paleta.core.palette.PaletteExportFormat
import ru.diamko.paleta.core.palette.PaletteExportFormatter
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.domain.repository.PaletteRepository

class FakePaletteRepository(
    private val tokenStore: TokenStore,
) : PaletteRepository {

    override suspend fun getPalettes(): List<Palette> = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.palettesForUser(token)
    }

    override suspend fun createPalette(name: String, colors: List<String>): Palette = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.createPalette(token, name.trim(), colors)
    }

    override suspend fun renamePalette(id: Long, name: String): Palette = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.renamePalette(token, id, name.trim())
    }

    override suspend fun deletePalette(id: Long) = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.deletePalette(token, id)
    }

    override suspend fun generateFromImage(
        fileName: String,
        imageBytes: ByteArray,
        colorCount: Int,
    ): List<String> = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalArgumentException("Не удалось прочитать изображение")
        BitmapPaletteExtractor.extractFromBitmap(bitmap, colorCount)
    }

    override suspend fun exportPalette(
        name: String,
        colors: List<String>,
        format: String,
    ): PaletteExportFile = withContext(Dispatchers.Default) {
        val exportFormat = PaletteExportFormat.entries.firstOrNull {
            it.ext.equals(format, ignoreCase = true)
        } ?: throw IllegalArgumentException("Неподдерживаемый формат экспорта")

        val payload = PaletteExportFormatter.format(
            name = name,
            colors = colors,
            format = exportFormat,
        )

        PaletteExportFile(
            fileName = payload.fileName,
            mimeType = payload.mimeType,
            bytes = payload.bytes,
        )
    }

    private suspend fun requireAccessToken(): String {
        return tokenStore.readAccessToken()
            ?: throw IllegalStateException("Нужно войти в аккаунт")
    }
}
