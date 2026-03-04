package ru.diamko.paleta.core.palette

object HexColors {
    private val hexRegex = Regex("^#[0-9A-F]{6}$")

    fun parse(raw: String): List<String>? {
        val colors = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.uppercase() }
        return normalize(colors)
    }

    fun normalize(colors: List<String>): List<String>? {
        if (colors.size !in 3..15) return null
        if (colors.any { !hexRegex.matches(it.uppercase()) }) return null
        return colors.map { it.uppercase() }
    }
}
