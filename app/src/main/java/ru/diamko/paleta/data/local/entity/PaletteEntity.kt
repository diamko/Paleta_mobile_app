/**
 * Модуль: PaletteEntity.
 * Назначение: Room-сущность палитры для локального хранения.
 */
package ru.diamko.paleta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "palettes")
data class PaletteEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val colorsJson: String,
    val createdAtIso: String,
    val isSynced: Boolean = true,
    val pendingAction: String? = null,
)
