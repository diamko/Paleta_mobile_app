/**
 * Модуль: LastImageStorage.
 * Назначение: Сохранение и восстановление последнего загруженного изображения.
 */
package ru.diamko.paleta.core.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LastImageStorage(
    private val context: Context,
) {
    private val fileName = "last_image.bin"

    suspend fun save(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            runCatching {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                    output.write(bytes)
                }
            }
        }
    }

    suspend fun read(): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                context.openFileInput(fileName).use { input ->
                    input.readBytes()
                }
            } catch (_: IOException) {
                null
            }
        }
    }
}
