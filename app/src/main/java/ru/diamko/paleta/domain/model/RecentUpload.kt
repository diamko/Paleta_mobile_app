package ru.diamko.paleta.domain.model

data class RecentUpload(
    val id: Long,
    val filename: String,
    val createdAtIso: String,
    val url: String,
)
