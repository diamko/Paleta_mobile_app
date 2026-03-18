/**
 * Модуль: DeletePaletteUseCase.
 * Назначение: Use-case: удаление палитры.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.PaletteRepository

class DeletePaletteUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(id: Long) {
        paletteRepository.deletePalette(id)
    }
}
