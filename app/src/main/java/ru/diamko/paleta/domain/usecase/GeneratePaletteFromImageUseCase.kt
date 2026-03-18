/**
 * Модуль: GeneratePaletteFromImageUseCase.
 * Назначение: Use-case: генерация палитры из загруженного изображения.
 */
package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.repository.PaletteRepository

class GeneratePaletteFromImageUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(
        fileName: String,
        imageBytes: ByteArray,
        colorCount: Int,
    ): List<String> {
        return paletteRepository.generateFromImage(
            fileName = fileName,
            imageBytes = imageBytes,
            colorCount = colorCount,
        )
    }
}
