/**
 * Модуль: RecentUpload.
 * Назначение: Модель недавней загрузки изображения.
 */
package ru.diamko.paleta.domain.model

data class RecentUpload(
    val id: Long,
    val filename: String,
    val createdAtIso: String,
    val url: String,
)
