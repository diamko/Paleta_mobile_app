package ru.diamko.paleta.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.network.NetworkError
import ru.diamko.paleta.data.remote.api.PaletteApi
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.CreatePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.ExportPaletteRequestDto
import ru.diamko.paleta.data.remote.dto.PaletteDto
import ru.diamko.paleta.data.remote.dto.PaletteListDataDto
import ru.diamko.paleta.data.remote.dto.RenamePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.UploadImageDataDto
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
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

    override suspend fun generateFromImage(
        fileName: String,
        imageBytes: ByteArray,
        colorCount: Int,
    ): List<String> = withContext(Dispatchers.IO) {
        val imagePart = MultipartBody.Part.createFormData(
            name = "image",
            filename = fileName.ifBlank { "upload.jpg" },
            body = imageBytes.toRequestBody("application/octet-stream".toMediaType()),
        )
        val colorCountBody = colorCount.toString().toRequestBody("text/plain".toMediaType())

        val envelope = paletteApi.uploadImage(
            image = imagePart,
            colorCount = colorCountBody,
        )
        envelope.unwrapUpload("Не удалось извлечь палитру из изображения").palette
    }

    override suspend fun exportPalette(
        name: String,
        colors: List<String>,
        format: String,
    ): PaletteExportFile = withContext(Dispatchers.IO) {
        val response = paletteApi.exportPalette(
            format = format.lowercase(),
            request = ExportPaletteRequestDto(colors = colors),
        )

        if (!response.isSuccessful) {
            throw NetworkError("Не удалось экспортировать палитру (${response.code()})")
        }

        val body = response.body() ?: throw NetworkError("Пустой ответ сервера при экспорте")
        val bytes = body.bytes()
        val mimeType = response.headers()["Content-Type"]
            ?: body.contentType()?.toString()
            ?: "application/octet-stream"
        val fileName = parseFileName(response.headers()["Content-Disposition"])
            ?: "${name.ifBlank { "palette" }}.${format.lowercase()}"

        PaletteExportFile(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
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

    private fun ApiEnvelope<UploadImageDataDto>.unwrapUpload(defaultMessage: String): UploadImageDataDto {
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

    private fun parseFileName(contentDisposition: String?): String? {
        val value = contentDisposition ?: return null
        val regex = Regex("filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?")
        val match = regex.find(value) ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value)?.trim()
    }
}
