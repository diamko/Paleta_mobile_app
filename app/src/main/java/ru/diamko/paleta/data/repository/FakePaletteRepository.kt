package ru.diamko.paleta.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.diamko.paleta.core.storage.TokenStore
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.repository.PaletteRepository

class FakePaletteRepository(
    private val tokenStore: TokenStore,
) : PaletteRepository {

    override suspend fun getPalettes(): List<Palette> = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.palettesForUser(token)
    }

    override suspend fun createPalette(name: String, colors: List<String>): Palette = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.createPalette(token, name.trim(), colors)
    }

    override suspend fun renamePalette(id: Long, name: String): Palette = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.renamePalette(token, id, name.trim())
    }

    override suspend fun deletePalette(id: Long) = withContext(Dispatchers.IO) {
        val token = requireAccessToken()
        FakeBackend.deletePalette(token, id)
    }

    private suspend fun requireAccessToken(): String {
        return tokenStore.readAccessToken()
            ?: throw IllegalStateException("Нужно войти в аккаунт")
    }
}
