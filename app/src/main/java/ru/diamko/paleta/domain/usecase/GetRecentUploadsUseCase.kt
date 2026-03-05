package ru.diamko.paleta.domain.usecase

import ru.diamko.paleta.domain.model.RecentUpload
import ru.diamko.paleta.domain.repository.PaletteRepository

class GetRecentUploadsUseCase(
    private val paletteRepository: PaletteRepository,
) {
    suspend operator fun invoke(days: Int = 7): List<RecentUpload> {
        return paletteRepository.getRecentUploads(days)
    }
}
