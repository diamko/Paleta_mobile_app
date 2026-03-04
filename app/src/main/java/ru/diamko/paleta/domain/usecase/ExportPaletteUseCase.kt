package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.domain.repository.PaletteRepository

class ExportPaletteUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(
        name: String,
        colors: List<String>,
        format: String,
    ): PaletteExportFile {
        return paletteRepository.exportPalette(
            name = name,
            colors = colors,
            format = format,
        )
    }
}
