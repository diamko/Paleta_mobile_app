/**
 * Модуль: Routes.
 * Назначение: Перечисление маршрутов навигации.
 */
package ru.diamko.paleta.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password"
    const val PALETTES = "palettes"
    const val GENERATE_RANDOM = "generate/random"
    const val GENERATE_IMAGE = "generate/image"
    const val GENERATE = GENERATE_RANDOM
    const val SETTINGS = "settings"
    const val PROFILE_EDIT = "profile_edit"
    const val PASSWORD_CHANGE = "password_change"
    const val FAQ = "faq"
    const val PALETTE_EDITOR = "palette_editor/{paletteId}"

    fun paletteEditor(paletteId: String): String = "palette_editor/$paletteId"
}
