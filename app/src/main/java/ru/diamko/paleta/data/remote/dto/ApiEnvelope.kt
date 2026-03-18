/**
 * Модуль: ApiEnvelope.
 * Назначение: Обёртка ответа API: success, data, error.
 */
package ru.diamko.paleta.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null,
)
