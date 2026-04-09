/**
 * Модуль: RemotePaletteRepository.
 * Назначение: Реализация репозитория палитр через REST API.
 */
package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import ru.diamko.paleta.core.network.NetworkError
import ru.diamko.paleta.data.remote.api.PaletteApi
import ru.diamko.paleta.data.remote.dto.ApiEnvelope
import ru.diamko.paleta.data.remote.dto.CreatePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.ExportPaletteRequestDto
import ru.diamko.paleta.data.remote.dto.LegacyUploadImageResponseDto
import ru.diamko.paleta.data.remote.dto.PaletteDto
import ru.diamko.paleta.data.remote.dto.PaletteListDataDto
import ru.diamko.paleta.data.remote.dto.RecentUploadDto
import ru.diamko.paleta.data.remote.dto.RecentUploadsDataDto
import ru.diamko.paleta.data.remote.dto.RenamePaletteRequestDto
import ru.diamko.paleta.data.remote.dto.UploadImageDataDto
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.domain.model.RecentUpload
import ru.diamko.paleta.domain.repository.PaletteRepository
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class RemotePaletteRepository(
    private val paletteApi: PaletteApi,
) : PaletteRepository {
    private val rawHttpClient = OkHttpClient()

    override suspend fun getPalettes(): List<Palette> = withContext(Dispatchers.IO) {
        val envelope = paletteApi.getPalettes()
        envelope.unwrapList("Не удалось получить список палитр")
            .items
            .map { it.toDomain() }
    }

    override suspend fun getRecentUploads(days: Int): List<RecentUpload> = withContext(Dispatchers.IO) {
        val envelope = paletteApi.getRecentUploads(days = days.coerceIn(1, 30))
        envelope.unwrapRecentUploads("Не удалось загрузить недавние изображения")
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
        fun createImagePart() = MultipartBody.Part.createFormData(
            name = "image",
            filename = fileName.ifBlank { "upload.jpg" },
            body = imageBytes.toRequestBody("application/octet-stream".toMediaType()),
        )
        fun createColorCountBody() = colorCount.toString().toRequestBody("text/plain".toMediaType())

        try {
            val envelope = paletteApi.uploadImage(
                image = createImagePart(),
                colorCount = createColorCountBody(),
            )
            return@withContext envelope.unwrapUpload("Не удалось извлечь палитру из изображения").palette
        } catch (error: HttpException) {
            if (error.code() != 404) {
                throw NetworkError("Не удалось извлечь палитру из изображения (${error.code()})")
            }
        }

        val legacy = paletteApi.uploadImageLegacy(
            image = createImagePart(),
            colorCount = createColorCountBody(),
        )
        legacy.unwrapLegacyUpload("Не удалось извлечь палитру из изображения")
    }

    override suspend fun generateFromImageUrl(imageUrl: String, colorCount: Int): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(imageUrl)
            .get()
            .build()
        val response = rawHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw NetworkError("Не удалось загрузить изображение (${response.code})")
        }
        val bytes = response.body?.bytes() ?: throw NetworkError("Пустой ответ сервера при загрузке изображения")
        val fileName = imageUrl.substringAfterLast('/').ifBlank { "upload.jpg" }
        generateFromImage(fileName = fileName, imageBytes = bytes, colorCount = colorCount)
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
        val fileName = buildExportFileName(
            requestedName = name,
            format = format,
            fallbackHeader = response.headers()["Content-Disposition"],
        )

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

    private fun ApiEnvelope<RecentUploadsDataDto>.unwrapRecentUploads(defaultMessage: String): RecentUploadsDataDto {
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

    private fun LegacyUploadImageResponseDto.unwrapLegacyUpload(defaultMessage: String): List<String> {
        if (success && !palette.isNullOrEmpty()) {
            return palette
        }
        throw NetworkError(error ?: defaultMessage)
    }

    private fun PaletteDto.toDomain(): Palette {
        return Palette(
            id = id,
            name = name,
            colors = colors,
            createdAtIso = created_at,
        )
    }

    private fun RecentUploadDto.toDomain(): RecentUpload {
        return RecentUpload(
            id = id,
            filename = filename,
            createdAtIso = created_at,
            url = url,
        )
    }

    private fun parseFileName(contentDisposition: String?): String? {
        val value = contentDisposition ?: return null
        val regex = Regex("filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?")
        val match = regex.find(value) ?: return null
        val raw = (match.groups[1]?.value ?: match.groups[2]?.value)?.trim() ?: return null
        return runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8.name()) }.getOrDefault(raw)
    }

    private fun buildExportFileName(
        requestedName: String,
        format: String,
        fallbackHeader: String?,
    ): String {
        val extension = format.trim().lowercase().ifBlank { "json" }
        val normalized = requestedName
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "palette" }
        if (normalized.contains('.')) {
            return normalized
        }
        val parsedHeaderName = parseFileName(fallbackHeader)
        val parsedHeaderExt = parsedHeaderName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
        val ext = parsedHeaderExt ?: extension
        return "$normalized.$ext"
    }
}
