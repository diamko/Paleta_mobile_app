package ru.diamko.paleta

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.diamko.paleta.data.repository.FakeBackend

class FakeBackendSmokeTest {
    @Test
    fun loginAndReadPalettes() {
        val (user, tokens) = FakeBackend.login("demo", "Demo@12345")
        val palettes = FakeBackend.palettesForUser(tokens.accessToken)

        assertTrue(user.id > 0)
        assertTrue(palettes.isNotEmpty())
    }
}
