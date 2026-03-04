package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.network.NetworkError
import ru.diamko.paleta.data.remote.api.PaletteApi
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.CreatePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.PaletteDto
import ru.diamko.paleta.data.remote.dto.PaletteListDataDto
import ru.diamko.paleta.data.remote.dto.RenamePaletteRequestDto
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.repository.PaletteRepository

class RemotePaletteRepository(
    private val paletteApi: PaletteApi,
) : PaletteRepository {

    override suspend fun getPalettes(): List<Palette> = withContext(Dispatchers.IO) {
        val envelope = paletteApi.getPalettes()
        envelope.unwrapList("Не удалось получить список палитр")
            .items
            .map { it.toDomain() }
    }

    override suspend fun createPalette(name: String, colors: List<String>): Palette = withContext(Dispatchers.IO) {
        val envelope = paletteApi.createPalette(
            CreatePaletteRequestDto(
                name = name,
                colors = colors,
                lang = "ru",
            ),
        )
        envelope.unwrapOne("Не удалось создать палитру").toDomain()
    }

    override suspend fun renamePalette(id: Long, name: String): Palette = withContext(Dispatchers.IO) {
        val envelope = paletteApi.renamePalette(
            paletteId = id,
            request = RenamePaletteRequestDto(name = name),
        )
        envelope.unwrapOne("Не удалось переименовать палитру").toDomain()
    }

    override suspend fun deletePalette(id: Long) = withContext(Dispatchers.IO) {
        val envelope = paletteApi.deletePalette(id)
        if (!envelope.success) {
            throw NetworkError(envelope.error?.message ?: "Не удалось удалить палитру")
        }
    }

    private fun ApiEnvelope<PaletteListDataDto>.unwrapList(defaultMessage: String): PaletteListDataDto {
        if (success && data != null) {
            return data
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun ApiEnvelope<PaletteDto>.unwrapOne(defaultMessage: String): PaletteDto {
        if (success && data != null) {
            return data
        }
        throw NetworkError(error?.message ?: defaultMessage)
    }

    private fun PaletteDto.toDomain(): Palette {
        return Palette(
            id = id,
            name = name,
            colors = colors,
            createdAtIso = created_at,
        )
    }
}
