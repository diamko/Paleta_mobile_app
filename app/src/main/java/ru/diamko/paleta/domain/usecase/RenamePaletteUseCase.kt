/**
 * Модуль: RenamePaletteUseCase.
 * Назначение: Use-case: переименование палитры.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.repository.PaletteRepository

class RenamePaletteUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(id: Long, name: String): Palette {
        return paletteRepository.renamePalette(id, name)
    }
}
