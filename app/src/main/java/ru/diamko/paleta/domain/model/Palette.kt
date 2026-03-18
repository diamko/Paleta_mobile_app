/**
 * Модуль: Palette.
 * Назначение: Доменная модель палитры: id, название, список цветов, дата.
 */
package ru.diamko.paleta.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Palette(
    val id: Long,
    val name: String,
    val colors: List<String>,
    val createdAtIso: String,
)
