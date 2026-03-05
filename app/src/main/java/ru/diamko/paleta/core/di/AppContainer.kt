package ru.diamko.paleta.core.di

import android.content.Context
import ru.diamko.paleta.BuildConfig
import ru.diamko.paleta.core.network.NetworkModule
import ru.diamko.paleta.core.storage.DataStoreLocaleStore
import ru.diamko.paleta.core.storage.DataStoreTokenStore
import ru.diamko.paleta.core.storage.LocaleStore
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.data.repository.FakeAuthRepository
import ru.diamko.paleta.data.repository.FakePaletteRepository
import ru.diamko.paleta.data.repository.RemoteAuthRepository
import ru.diamko.paleta.data.repository.RemotePaletteRepository
import ru.diamko.paleta.data.repository.RepositoryMode
import ru.diamko.paleta.domain.repository.AuthRepository
import ru.diamko.paleta.domain.repository.PaletteRepository

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun getString(resId: Int): String = appContext.getString(resId)

    val tokenStore: TokenStore by lazy {
        DataStoreTokenStore(appContext)
    }

    val localeStore: LocaleStore by lazy {
        DataStoreLocaleStore(appContext)
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

    val authRepository: AuthRepository by lazy {
        when (mode) {
            RepositoryMode.REMOTE -> RemoteAuthRepository(networkModule.authApi, tokenStore)
            RepositoryMode.FAKE -> FakeAuthRepository(tokenStore)
        }
    }

    val paletteRepository: PaletteRepository by lazy {
        when (mode) {
            RepositoryMode.REMOTE -> RemotePaletteRepository(networkModule.paletteApi)
            RepositoryMode.FAKE -> FakePaletteRepository(tokenStore)
        }
    }
}
