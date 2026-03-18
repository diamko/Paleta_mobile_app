/**
 * Модуль: NetworkModule.
 * Назначение: Конфигурация Retrofit и OkHttp; создание API-интерфейсов.
 */
package ru.diamko.paleta.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.data.remote.api.AuthApi
import ru.diamko.paleta.data.remote.api.PaletteApi

class NetworkModule(
    tokenStore: TokenStore,
    baseUrl: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val paletteApi: PaletteApi = retrofit.create(PaletteApi::class.java)
}
