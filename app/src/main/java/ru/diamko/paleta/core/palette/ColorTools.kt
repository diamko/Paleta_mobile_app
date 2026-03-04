package ru.diamko.paleta.core.palette

import android.graphics.Color
import java.util.Locale

object ColorTools {
    fun colorIntToHex(colorInt: Int): String {
        return String.format(
            Locale.US,
            "#%02X%02X%02X",
            Color.red(colorInt),
            Color.green(colorInt),
            Color.blue(colorInt),
        )
    }

    fun hexToColorInt(hex: String): Int? {
        return runCatching { Color.parseColor(hex) }.getOrNull()
    }
}
