package ru.diamko.paleta.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "paleta_theme")

class DataStoreThemeStore(
    private val context: Context,
) : ThemeStore {

    override val isDarkThemeFlow: Flow<Boolean?> = context.themeDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs[Keys.IS_DARK] }

    override suspend fun readIsDarkTheme(): Boolean? {
        return context.themeDataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()[Keys.IS_DARK]
    }

    override suspend fun saveIsDarkTheme(isDark: Boolean?) {
        context.themeDataStore.edit { prefs ->
            if (isDark == null) {
                prefs.remove(Keys.IS_DARK)
            } else {
                prefs[Keys.IS_DARK] = isDark
            }
        }
    }

    private object Keys {
        val IS_DARK = booleanPreferencesKey("is_dark")
    }
}
