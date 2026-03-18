/**
 * Модуль: LocaleStore.
 * Назначение: Интерфейс хранилища языковых настроек.
 */
package ru.diamko.paleta.core.storage

interface LocaleStore {
    suspend fun readLanguageTag(): String?
    suspend fun saveLanguageTag(languageTag: String)
}
