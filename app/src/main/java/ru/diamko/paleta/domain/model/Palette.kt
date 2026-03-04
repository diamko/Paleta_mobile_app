package ru.diamko.paleta.domain.model

data class Palette(
    val id: Long,
    val name: String,
    val colors: List<String>,
    val createdAtIso: String,
)
