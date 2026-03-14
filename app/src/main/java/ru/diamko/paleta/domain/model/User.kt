package ru.diamko.paleta.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class User(
    val id: Long,
    val username: String,
    val email: String,
)
