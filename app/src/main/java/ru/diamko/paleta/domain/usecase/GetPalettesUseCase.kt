/**
 * Модуль: GetPalettesUseCase.
 * Назначение: Use-case: получение списка палитр пользователя.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.repository.PaletteRepository

class GetPalettesUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(): List<Palette> {
        return paletteRepository.getPalettes()
    }
}
