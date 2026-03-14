package ru.diamko.paleta.core.di

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import ru.diamko.paleta.BuildConfig
import ru.diamko.paleta.core.network.NetworkModule
import ru.diamko.paleta.core.network.NetworkMonitor
import ru.diamko.paleta.core.storage.DataStoreLocaleStore
import ru.diamko.paleta.core.storage.DataStoreThemeStore
import ru.diamko.paleta.core.storage.DataStoreTokenStore
import ru.diamko.paleta.core.storage.DataStoreUserStore
import ru.diamko.paleta.core.storage.LocaleStore
import ru.diamko.paleta.core.storage.ThemeStore
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.core.storage.UserStore
import ru.diamko.paleta.data.local.PaletaDatabase
import ru.diamko.paleta.data.repository.FakeAuthRepository
import ru.diamko.paleta.data.repository.FakePaletteRepository
import ru.diamko.paleta.data.repository.OfflinePaletteRepository
import ru.diamko.paleta.data.repository.RemoteAuthRepository
import ru.diamko.paleta.data.repository.RemotePaletteRepository
import ru.diamko.paleta.data.repository.RepositoryMode
import ru.diamko.paleta.domain.repository.AuthRepository
import ru.diamko.paleta.domain.repository.PaletteRepository

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun getString(resId: Int): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return appContext.getString(resId)
        val config = Configuration(appContext.resources.configuration)
        config.setLocales(android.os.LocaleList(locales[0]!!))
        return appContext.createConfigurationContext(config).getString(resId)
    }

    val tokenStore: TokenStore by lazy {
        DataStoreTokenStore(appContext)
    }

    val localeStore: LocaleStore by lazy {
        DataStoreLocaleStore(appContext)
    }

    val themeStore: ThemeStore by lazy {
        DataStoreThemeStore(appContext)
    }

    private val mode: RepositoryMode by lazy {
        RepositoryMode.from(BuildConfig.REPOSITORY_MODE)
    }

    private val networkModule: NetworkModule by lazy {
        NetworkModule(
            tokenStore = tokenStore,
            baseUrl = BuildConfig.API_BASE_URL,
        )
    }

    private val database: PaletaDatabase by lazy {
        Room.databaseBuilder(appContext, PaletaDatabase::class.java, "paleta.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(appContext)
    }

    val userStore: UserStore by lazy {
        DataStoreUserStore(appContext)
    }

    val authRepository: AuthRepository by lazy {
        when (mode) {
            RepositoryMode.REMOTE -> RemoteAuthRepository(networkModule.authApi, tokenStore, userStore)
            RepositoryMode.FAKE -> FakeAuthRepository(tokenStore)
        }
    }

    val paletteRepository: PaletteRepository by lazy {
        when (mode) {
            RepositoryMode.REMOTE -> OfflinePaletteRepository(
                remote = RemotePaletteRepository(networkModule.paletteApi),
                dao = database.paletteDao(),
                networkMonitor = networkMonitor,
            )
            RepositoryMode.FAKE -> FakePaletteRepository(tokenStore)
        }
    }
}
