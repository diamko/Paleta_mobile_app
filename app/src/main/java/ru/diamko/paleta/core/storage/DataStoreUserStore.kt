package ru.diamko.paleta.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import ru.diamko.paleta.domain.model.User
import java.io.IOException

private val Context.userDataStore by preferencesDataStore(name = "paleta_user")

class DataStoreUserStore(
    private val context: Context,
) : UserStore {

    override suspend fun saveUser(user: User) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USERNAME] = user.username
            prefs[Keys.EMAIL] = user.email
        }
    }

    override suspend fun readUser(): User? {
        val prefs = context.userDataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()
        val id = prefs[Keys.USER_ID] ?: return null
        val username = prefs[Keys.USERNAME] ?: return null
        val email = prefs[Keys.EMAIL] ?: return null
        return User(id = id, username = username, email = email)
    }

    override suspend fun clearUser() {
        context.userDataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.EMAIL)
        }
    }

    private object Keys {
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("user_username")
        val EMAIL = stringPreferencesKey("user_email")
    }
}
