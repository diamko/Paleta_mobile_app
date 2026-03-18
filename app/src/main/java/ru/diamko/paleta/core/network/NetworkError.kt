/**
 * Модуль: NetworkError.
 * Назначение: Обёртка исключения для сетевых ошибок API.
 */
package ru.diamko.paleta.core.network

class NetworkError(message: String) : Exception(message)
