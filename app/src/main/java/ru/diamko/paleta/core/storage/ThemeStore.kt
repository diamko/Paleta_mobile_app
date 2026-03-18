/**
 * Модуль: ThemeStore.
 * Назначение: Интерфейс хранилища темы оформления.
 */
package ru.diamko.paleta.core.storage

import kotlinx.coroutines.flow.Flow

interface ThemeStore {
    val isDarkThemeFlow: Flow<Boolean?>
    suspend fun readIsDarkTheme(): Boolean?
    suspend fun saveIsDarkTheme(isDark: Boolean?)
}
