/**
 * Модуль: RepositoryMode.
 * Назначение: Перечисление режимов репозитория: FAKE / REMOTE.
 */
package ru.diamko.paleta.data.repository

enum class RepositoryMode {
    FAKE,
    REMOTE;

    companion object {
        fun from(raw: String): RepositoryMode {
            return when (raw.trim().lowercase()) {
                "remote" -> REMOTE
                else -> FAKE
            }
        }
    }
}
