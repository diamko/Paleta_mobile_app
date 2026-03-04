package ru.diamko.paleta.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PALETTES = "palettes"
    const val GENERATE = "generate"
    const val SETTINGS = "settings"
    const val PALETTE_EDITOR = "palette_editor/{paletteId}"

    fun paletteEditor(paletteId: String): String = "palette_editor/$paletteId"
}
