package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.repository.PaletteRepository

class CreatePaletteUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(name: String, colors: List<String>): Palette {
        return paletteRepository.createPalette(name, colors)
    }
}
