package ru.diamko.paleta.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaletteDto(
    val id: Long,
    val name: String,
    val colors: List<String>,
    val created_at: String,
)

@Serializable
data class PaletteListDataDto(
    val items: List<PaletteDto>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

@Serializable
data class CreatePaletteRequestDto(
    val name: String,
    val colors: List<String>,
    val lang: String = "ru",
)

@Serializable
data class RenamePaletteRequestDto(
    val name: String,
)

@Serializable
data class UploadImageDataDto(
    val filename: String,
    val palette: List<String>,
)

@Serializable
data class ExportPaletteRequestDto(
    val colors: List<String>,
)
