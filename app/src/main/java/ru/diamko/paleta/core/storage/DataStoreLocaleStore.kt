package ru.diamko.paleta.core.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.localeStore by preferencesDataStore(name = "paleta_locale")

class DataStoreLocaleStore(
    private val context: Context,
) : LocaleStore {
    override suspend fun readLanguageTag(): String? {
        return readPreference(Keys.LANGUAGE_TAG)
    }

    override suspend fun saveLanguageTag(languageTag: String) {
        context.localeStore.edit { prefs ->
            prefs[Keys.LANGUAGE_TAG] = languageTag
        }
    }

    private suspend fun readPreference(key: Preferences.Key<String>): String? {
        return context.localeStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .first()[key]
    }

    private object Keys {
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }
}
