/**
 * Модуль: PaletteSyncWorker.
 * Назначение: WorkManager-задача для фоновой синхронизации палитр.
 */
package ru.diamko.paleta.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.diamko.paleta.PaletaApplication
import ru.diamko.paleta.data.repository.OfflinePaletteRepository

class PaletteSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PaletaApplication ?: return Result.failure()
        val repo = app.container.paletteRepository
        if (repo is OfflinePaletteRepository) {
            return try {
                repo.syncPendingChanges()
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "palette_sync"
    }
}
