package ru.diamko.paleta.data.repository

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import ru.diamko.paleta.domain.model.AuthTokens
import ru.diamko.paleta.domain.model.Palette
import ru.diamko.paleta.domain.model.User

object FakeBackend {
    private data class UserRecord(
        val id: Long,
        var username: String,
        val email: String,
        var password: String,
    )

    private val lock = Any()
    private val userIdSeq = AtomicLong(2)
    private val paletteIdSeq = AtomicLong(4)

    private val users = mutableListOf(
        UserRecord(id = 1, username = "demo", email = "demo@paleta.ru", password = "Demo@12345"),
    )

    private val palettesByUser = mutableMapOf(
        1L to mutableListOf(
            Palette(
                id = 1,
                name = "Тёплая база",
                colors = listOf("#F26430", "#F8CFA6", "#FFE8D6"),
                createdAtIso = Instant.now().toString(),
            ),
            Palette(
                id = 2,
                name = "Морской акцент",
                colors = listOf("#003049", "#2A9D8F", "#E9C46A", "#F4A261"),
                createdAtIso = Instant.now().toString(),
            ),
            Palette(
                id = 3,
                name = "Нейтральная",
                colors = listOf("#1F1F1F", "#777777", "#D9D9D9"),
                createdAtIso = Instant.now().toString(),
            ),
        ),
    )

    private val accessSessions = mutableMapOf<String, Long>()
    private val refreshSessions = mutableMapOf<String, Long>()

    fun login(login: String, password: String): Pair<User, AuthTokens> = synchronized(lock) {
        val record = users.firstOrNull {
            (it.username.equals(login, ignoreCase = true) || it.email.equals(login, ignoreCase = true)) &&
                it.password == password
        } ?: throw IllegalArgumentException("Неверный логин или пароль")

        val tokens = issueTokens(record.id)
        toUser(record) to tokens
    }

    fun register(username: String, email: String, password: String): Pair<User, AuthTokens> = synchronized(lock) {
        if (users.any { it.username.equals(username, ignoreCase = true) }) {
            throw IllegalArgumentException("Пользователь с таким именем уже существует")
        }

        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            throw IllegalArgumentException("Этот email уже используется")
        }

        val id = userIdSeq.getAndIncrement()
        val userRecord = UserRecord(
            id = id,
            username = username,
            email = email,
            password = password,
        )
        users += userRecord
        palettesByUser[id] = mutableListOf()

        val tokens = issueTokens(id)
        toUser(userRecord) to tokens
    }

    fun userByAccessToken(accessToken: String): User? = synchronized(lock) {
        val userId = accessSessions[accessToken] ?: return null
        val user = users.firstOrNull { it.id == userId } ?: return null
        toUser(user)
    }

    fun revoke(accessToken: String?, refreshToken: String?) = synchronized(lock) {
        if (!accessToken.isNullOrBlank()) {
            accessSessions.remove(accessToken)
        }
        if (!refreshToken.isNullOrBlank()) {
            refreshSessions.remove(refreshToken)
        }
    }

    fun palettesForUser(accessToken: String): List<Palette> = synchronized(lock) {
        val userId = accessSessions[accessToken] ?: throw IllegalStateException("Сессия истекла")
        return (palettesByUser[userId] ?: mutableListOf())
            .sortedByDescending { it.createdAtIso }
            .toList()
    }

    fun createPalette(accessToken: String, name: String, colors: List<String>): Palette = synchronized(lock) {
        val userId = accessSessions[accessToken] ?: throw IllegalStateException("Сессия истекла")
        validateColors(colors)

        val palette = Palette(
            id = paletteIdSeq.getAndIncrement(),
            name = name.ifBlank { "Моя палитра" },
            colors = colors,
            createdAtIso = Instant.now().toString(),
        )

        val list = palettesByUser.getOrPut(userId) { mutableListOf() }
        if (list.any { it.name.equals(palette.name, ignoreCase = true) }) {
            throw IllegalArgumentException("Палитра с таким названием уже существует")
        }
        list += palette
        palette
    }

    fun renamePalette(accessToken: String, paletteId: Long, name: String): Palette = synchronized(lock) {
        val userId = accessSessions[accessToken] ?: throw IllegalStateException("Сессия истекла")
        val list = palettesByUser[userId] ?: throw IllegalArgumentException("Палитра не найдена")
        val index = list.indexOfFirst { it.id == paletteId }
        if (index < 0) throw IllegalArgumentException("Палитра не найдена")
        if (list.any { it.id != paletteId && it.name.equals(name, ignoreCase = true) }) {
            throw IllegalArgumentException("Палитра с таким названием уже существует")
        }

        val updated = list[index].copy(name = name.ifBlank { list[index].name })
        list[index] = updated
        updated
    }

    fun deletePalette(accessToken: String, paletteId: Long): Unit = synchronized(lock) {
        val userId = accessSessions[accessToken] ?: throw IllegalStateException("Сессия истекла")
        val list = palettesByUser[userId] ?: return
        list.removeAll { it.id == paletteId }
    }

    private fun validateColors(colors: List<String>) {
        if (colors.size !in 3..15) {
            throw IllegalArgumentException("Палитра должна содержать от 3 до 15 цветов")
        }
        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        if (colors.any { !hexRegex.matches(it) }) {
            throw IllegalArgumentException("Некорректный HEX цвет")
        }
    }

    private fun issueTokens(userId: Long): AuthTokens {
        val access = "fake_access_${userId}_${Random.nextLong(100000, 999999)}"
        val refresh = "fake_refresh_${userId}_${Random.nextLong(100000, 999999)}"
        accessSessions[access] = userId
        refreshSessions[refresh] = userId
        return AuthTokens(accessToken = access, refreshToken = refresh)
    }

    private fun toUser(record: UserRecord): User {
        return User(id = record.id, username = record.username, email = record.email)
    }
}
