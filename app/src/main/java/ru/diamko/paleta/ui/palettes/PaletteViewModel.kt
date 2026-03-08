package ru.diamko.paleta.ui.palettes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.domain.model.RecentUpload
import ru.diamko.paleta.domain.usecase.CreatePaletteUseCase
import ru.diamko.paleta.domain.usecase.DeletePaletteUseCase
import ru.diamko.paleta.domain.usecase.ExportPaletteUseCase
import ru.diamko.paleta.domain.usecase.GeneratePaletteFromImageUrlUseCase
import ru.diamko.paleta.domain.usecase.GeneratePaletteFromImageUseCase
import ru.diamko.paleta.domain.usecase.GetPalettesUseCase
import ru.diamko.paleta.domain.usecase.GetRecentUploadsUseCase
import ru.diamko.paleta.domain.usecase.RenamePaletteUseCase

data class PaletteUiState(
    val isLoading: Boolean = false,
    val palettes: List<Palette> = emptyList(),
    val recentUploads: List<RecentUpload> = emptyList(),
    val error: String? = null,
)

class PaletteViewModel(
    private val getPalettesUseCase: GetPalettesUseCase,
    private val getRecentUploadsUseCase: GetRecentUploadsUseCase,
    private val createPaletteUseCase: CreatePaletteUseCase,
    private val renamePaletteUseCase: RenamePaletteUseCase,
    private val deletePaletteUseCase: DeletePaletteUseCase,
    private val generatePaletteFromImageUseCase: GeneratePaletteFromImageUseCase,
    private val generatePaletteFromImageUrlUseCase: GeneratePaletteFromImageUrlUseCase,
    private val exportPaletteUseCase: ExportPaletteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaletteUiState())
    val uiState: StateFlow<PaletteUiState> = _uiState.asStateFlow()

    fun loadPalettes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { getPalettesUseCase() }
                .onSuccess { palettes ->
                    _uiState.update { it.copy(isLoading = false, palettes = palettes, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Ошибка загрузки") }
                }
        }
    }

    fun loadRecentUploads(days: Int = 7, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { getRecentUploadsUseCase(days) }
                .onSuccess { uploads ->
                    _uiState.update { it.copy(recentUploads = uploads, error = null) }
                    onDone?.invoke()
                }
                .onFailure {
                    // Keep UX non-blocking: recent uploads are optional in flow.
                }
        }
    }

    fun createPalette(name: String, colors: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val existingPalettes = runCatching { getPalettesUseCase() }
                    .getOrElse { _uiState.value.palettes }
                val uniqueName = createUniquePaletteName(
                    requestedName = name,
                    existingNames = existingPalettes.map { it.name },
                )
                createPaletteUseCase(uniqueName, colors)
            }
                .onSuccess {
                    loadPalettes()
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Ошибка создания") }
                }
        }
    }

    fun renamePalette(id: Long, name: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { renamePaletteUseCase(id, name) }
                .onSuccess {
                    loadPalettes()
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Ошибка переименования") }
                }
        }
    }

    fun savePaletteChanges(
        id: Long,
        newName: String,
        newColors: List<String>,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val desiredName = newName.trim()
                if (desiredName.isBlank()) {
                    throw IllegalArgumentException("Palette name cannot be empty")
                }
                val normalizedColors = newColors.map { it.trim() }
                if (normalizedColors.isEmpty()) {
                    throw IllegalArgumentException("Palette colors are required")
                }

                val current = findPaletteById(id)
                    ?: throw IllegalArgumentException("Palette not found")
                val nameChanged = !current.name.equals(desiredName)
                val colorsChanged = current.colors != normalizedColors

                if (!nameChanged && !colorsChanged) {
                    return@runCatching
                }

                if (!colorsChanged) {
                    renamePaletteUseCase(id, desiredName)
                    return@runCatching
                }

                val tempName = "__tmp_${System.currentTimeMillis()}_${current.name}"
                    .take(90)
                    .replace(Regex("\\s+"), "_")
                renamePaletteUseCase(id, tempName)

                runCatching {
                    createPaletteUseCase(desiredName, normalizedColors)
                    deletePaletteUseCase(id)
                }.onFailure { createOrDeleteError ->
                    runCatching { renamePaletteUseCase(id, current.name) }
                    throw createOrDeleteError
                }
            }
                .onSuccess {
                    loadPalettes()
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "РћС€РёР±РєР° СЃРѕС…СЂР°РЅРµРЅРёСЏ") }
                }
        }
    }

    fun deletePalette(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { deletePaletteUseCase(id) }
                .onSuccess {
                    loadPalettes()
                    onDone()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Ошибка удаления") }
                }
        }
    }

    fun generateFromImage(
        fileName: String,
        imageBytes: ByteArray,
        colorCount: Int,
        onDone: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                generatePaletteFromImageUseCase(
                    fileName = fileName,
                    imageBytes = imageBytes,
                    colorCount = colorCount,
                )
            }.onSuccess { colors ->
                onDone(colors)
            }.onFailure { error ->
                onError(error.message ?: "Ошибка генерации палитры")
            }
        }
    }

    fun generateFromRecentUpload(
        imageUrl: String,
        colorCount: Int,
        onDone: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                generatePaletteFromImageUrlUseCase(
                    imageUrl = imageUrl,
                    colorCount = colorCount,
                )
            }.onSuccess { colors ->
                onDone(colors)
            }.onFailure { error ->
                onError(error.message ?: "Ошибка генерации из недавнего изображения")
            }
        }
    }

    fun exportPalette(
        name: String,
        colors: List<String>,
        format: String,
        onDone: (PaletteExportFile) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { exportPaletteUseCase(name = name, colors = colors, format = format) }
                .onSuccess { payload ->
                    onDone(payload)
                }
                .onFailure { error ->
                    onError(error.message ?: "Ошибка экспорта")
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun paletteById(id: Long): Palette? {
        return _uiState.value.palettes.firstOrNull { it.id == id }
    }

    private suspend fun findPaletteById(id: Long): Palette? {
        val fromState = _uiState.value.palettes.firstOrNull { it.id == id }
        if (fromState != null) return fromState
        return runCatching { getPalettesUseCase() }
            .getOrElse { emptyList() }
            .firstOrNull { it.id == id }
    }

    private fun createUniquePaletteName(
        requestedName: String,
        existingNames: List<String>,
    ): String {
        val base = requestedName.trim().ifBlank { "Моя палитра" }
        if (existingNames.none { it.equals(base, ignoreCase = true) }) {
            return base
        }

        val pattern = Regex("^${Regex.escape(base)}(?:\\s(\\d+))?$", RegexOption.IGNORE_CASE)
        val usedNumbers = existingNames.mapNotNull { name ->
            val match = pattern.matchEntire(name.trim()) ?: return@mapNotNull null
            match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
        }.toSet()

        var next = 1
        while (usedNumbers.contains(next)) {
            next += 1
        }
        return "$base $next"
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PaletteViewModel(
                        getPalettesUseCase = GetPalettesUseCase(container.paletteRepository),
                        getRecentUploadsUseCase = GetRecentUploadsUseCase(container.paletteRepository),
                        createPaletteUseCase = CreatePaletteUseCase(container.paletteRepository),
                        renamePaletteUseCase = RenamePaletteUseCase(container.paletteRepository),
                        deletePaletteUseCase = DeletePaletteUseCase(container.paletteRepository),
                        generatePaletteFromImageUseCase = GeneratePaletteFromImageUseCase(container.paletteRepository),
                        generatePaletteFromImageUrlUseCase = GeneratePaletteFromImageUrlUseCase(container.paletteRepository),
                        exportPaletteUseCase = ExportPaletteUseCase(container.paletteRepository),
                    ) as T
                }
            }
        }
    }
}
