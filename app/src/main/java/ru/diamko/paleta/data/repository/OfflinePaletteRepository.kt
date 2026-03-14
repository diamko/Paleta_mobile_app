package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.diamko.paleta.core.network.NetworkMonitor
import ru.diamko.paleta.data.local.dao.PaletteDao
import ru.diamko.paleta.data.local.entity.PaletteEntity
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.PaletteExportFile
import ru.diamko.paleta.domain.model.RecentUpload
import ru.diamko.paleta.domain.repository.PaletteRepository
import java.time.Instant

class OfflinePaletteRepository(
    private val remote: RemotePaletteRepository,
    private val dao: PaletteDao,
    private val networkMonitor: NetworkMonitor,
) : PaletteRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getPalettes(): List<Palette> = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline.value) {
            try {
                val remotePalettes = remote.getPalettes()
                cacheRemotePalettes(remotePalettes)
                return@withContext remotePalettes
            } catch (_: Exception) {
                // Fallback to cache on network error
            }
        }
        dao.getVisiblePalettes().map { it.toDomain() }
    }

    override suspend fun getRecentUploads(days: Int): List<RecentUpload> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value) return@withContext emptyList()
        remote.getRecentUploads(days)
    }

    override suspend fun createPalette(name: String, colors: List<String>): Palette = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline.value) {
            try {
                val palette = remote.createPalette(name, colors)
                dao.insert(palette.toEntity(isSynced = true))
                return@withContext palette
            } catch (_: Exception) {
                // Fallback to local creation
            }
        }
        val localId = -System.currentTimeMillis()
        val palette = Palette(
            id = localId,
            name = name,
            colors = colors,
            createdAtIso = Instant.now().toString(),
        )
        dao.insert(palette.toEntity(isSynced = false, pendingAction = "create"))
        palette
    }

    override suspend fun renamePalette(id: Long, name: String): Palette = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline.value) {
            try {
                val palette = remote.renamePalette(id, name)
                dao.insert(palette.toEntity(isSynced = true))
                return@withContext palette
            } catch (_: Exception) {
                // Fallback to local rename
            }
        }
        val cached = dao.getVisiblePalettes().firstOrNull { it.id == id }
            ?: throw IllegalStateException("Palette not found")
        val updated = cached.copy(
            name = name,
            isSynced = false,
            pendingAction = cached.pendingAction ?: "rename",
        )
        dao.insert(updated)
        updated.toDomain()
    }

    override suspend fun deletePalette(id: Long) = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline.value) {
            try {
                if (id > 0) remote.deletePalette(id)
                dao.deleteById(id)
                return@withContext
            } catch (_: Exception) {
                // Fallback to local delete
            }
        }
        if (id < 0) {
            dao.deleteById(id)
        } else {
            val cached = dao.getVisiblePalettes().firstOrNull { it.id == id }
            if (cached != null) {
                dao.insert(cached.copy(isSynced = false, pendingAction = "delete"))
            }
        }
    }

    override suspend fun generateFromImage(
        fileName: String,
        imageBytes: ByteArray,
        colorCount: Int,
    ): List<String> {
        return remote.generateFromImage(fileName, imageBytes, colorCount)
    }

    override suspend fun generateFromImageUrl(imageUrl: String, colorCount: Int): List<String> {
        return remote.generateFromImageUrl(imageUrl, colorCount)
    }

    override suspend fun exportPalette(
        name: String,
        colors: List<String>,
        format: String,
    ): PaletteExportFile {
        return remote.exportPalette(name, colors, format)
    }

    suspend fun syncPendingChanges() = withContext(Dispatchers.IO) {
        val pending = dao.getPendingChanges()
        for (entity in pending) {
            try {
                when (entity.pendingAction) {
                    "create" -> {
                        val colors: List<String> = json.decodeFromString(entity.colorsJson)
                        val created = remote.createPalette(entity.name, colors)
                        dao.deleteById(entity.id)
                        dao.insert(created.toEntity(isSynced = true))
                    }

                    "rename" -> {
                        val renamed = remote.renamePalette(entity.id, entity.name)
                        dao.insert(renamed.toEntity(isSynced = true))
                    }

                    "delete" -> {
                        remote.deletePalette(entity.id)
                        dao.deleteById(entity.id)
                    }

                    else -> {
                        dao.markSynced(entity.id)
                    }
                }
            } catch (_: Exception) {
                // Will retry on next sync
            }
        }
    }

    private suspend fun cacheRemotePalettes(palettes: List<Palette>) {
        val pending = dao.getPendingChanges()
        val pendingIds = pending.map { it.id }.toSet()
        dao.deleteAll()
        if (pending.isNotEmpty()) {
            dao.insertAll(pending)
        }
        val toCache = palettes.filter { it.id !in pendingIds }
        if (toCache.isNotEmpty()) {
            dao.insertAll(toCache.map { it.toEntity(isSynced = true) })
        }
    }

    private fun Palette.toEntity(isSynced: Boolean, pendingAction: String? = null): PaletteEntity {
        return PaletteEntity(
            id = id,
            name = name,
            colorsJson = json.encodeToString(colors),
            createdAtIso = createdAtIso,
            isSynced = isSynced,
            pendingAction = pendingAction,
        )
    }

    private fun PaletteEntity.toDomain(): Palette {
        return Palette(
            id = id,
            name = name,
            colors = json.decodeFromString(colorsJson),
            createdAtIso = createdAtIso,
        )
    }
}
