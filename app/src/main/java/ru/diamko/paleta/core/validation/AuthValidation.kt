/**
 * Модуль: AuthValidation.
 * Назначение: Валидация полей авторизации: email, имя пользователя, пароль.
 */
package ru.diamko.paleta.core.validation

object AuthValidation {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 64
    const val EMAIL_MAX = 254

    private val usernameRegex = Regex("^[A-Za-z0-9_.]{${USERNAME_MIN},${USERNAME_MAX}}$")

    fun sanitizeUsername(input: String): String {
        val filtered = buildString {
            input.forEach { ch ->
                if (isAsciiLetterOrDigit(ch) || ch == '_' || ch == '.') {
                    append(ch)
                }
            }
        }
        return filtered.take(USERNAME_MAX)
    }

    fun isValidUsername(input: String): Boolean {
        val trimmed = input.trim()
        return usernameRegex.matches(trimmed)
    }

    fun isValidEmail(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isBlank() || trimmed.contains(' ')) return false
        val atIndex = trimmed.indexOf('@')
        if (atIndex <= 0) return false
        val dotIndex = trimmed.lastIndexOf('.')
        if (dotIndex <= atIndex + 1) return false
        if (dotIndex >= trimmed.length - 1) return false
        return true
    }

    fun isValidPassword(input: String): Boolean {
        if (input.length !in PASSWORD_MIN..PASSWORD_MAX) return false
        val hasLetter = input.any { it.isLetter() }
        val hasDigit = input.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun isAsciiLetterOrDigit(ch: Char): Boolean {
        return (ch in 'a'..'z') || (ch in 'A'..'Z') || (ch in '0'..'9')
    }
}
