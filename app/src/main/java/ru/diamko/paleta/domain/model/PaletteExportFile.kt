package ru.diamko.paleta.domain.model

data class PaletteExportFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)
