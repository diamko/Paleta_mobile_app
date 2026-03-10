package ru.diamko.paleta.ui.palettes

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.R
import ru.diamko.paleta.domain.model.PaletteExportFile

suspend fun writeExportFile(
    context: Context,
    outputUri: Uri,
    payload: PaletteExportFile,
): Result<Unit> {
    return withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                stream.write(payload.bytes)
            } ?: error(context.getString(R.string.open_file_write_error))
        }
    }
}
