package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.PaletteRepository

class GeneratePaletteFromImageUrlUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(
        imageUrl: String,
        colorCount: Int,
    ): List<String> {
        return paletteRepository.generateFromImageUrl(
            imageUrl = imageUrl,
            colorCount = colorCount,
        )
    }
}
