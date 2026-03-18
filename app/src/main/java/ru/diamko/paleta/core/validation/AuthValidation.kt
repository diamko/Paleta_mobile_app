/**
 * Модуль: AuthValidation.
 * Назначение: Валидация полей авторизации: email, имя пользователя, пароль.
 */
package ru.diamko.paleta.core.validation

object AuthValidation {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 80
    const val PASSWORD_MIN = 10
    const val PASSWORD_MAX = 16
    const val EMAIL_MAX = 254

    fun sanitizeUsername(input: String): String {
        val filtered = buildString {
            input.forEach { ch ->
                if (!ch.isWhitespace()) append(ch)
            }
        }
        return filtered.take(USERNAME_MAX)
    }

    fun isValidUsername(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.length !in USERNAME_MIN..USERNAME_MAX) return false
        return trimmed.none { it.isWhitespace() }
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
        if (input.any { it.isWhitespace() }) return false
        val hasUpper = input.any { it.isUpperCase() }
        val hasLower = input.any { it.isLowerCase() }
        val hasDigit = input.any { it.isDigit() }
        val hasSpecial = input.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

}
