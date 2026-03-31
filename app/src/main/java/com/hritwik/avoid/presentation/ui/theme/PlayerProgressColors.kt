package com.hritwik.avoid.presentation.ui.theme

import androidx.compose.ui.graphics.Color

private val hexColorRegex = Regex("^#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")

data class PlayerProgressColorOption(
    val key: String,
    val label: String,
    val color: Color
)

val PlayerProgressColorOptions = listOf(
    PlayerProgressColorOption("purple", "Purple", Minsk),
    PlayerProgressColorOption("blue", "Blue", VoidBrightAzure),
    PlayerProgressColorOption("green", "Green", AmbientGreen),
    PlayerProgressColorOption("orange", "Orange", AmbientOrange),
    PlayerProgressColorOption("pink", "Pink", AmbientPink),
    PlayerProgressColorOption("red", "Red", AmbientRed),
    PlayerProgressColorOption("gold", "Gold", RatingGold),
    PlayerProgressColorOption("white", "White", Color.White)
)

fun resolvePlayerProgressColor(key: String): Color {
    val trimmed = key.trim()
    val normalizedHex = normalizeHexColor(trimmed)
    if (normalizedHex != null) {
        val hex = normalizedHex.removePrefix("#")
        val argb = if (hex.length == 6) {
            0xFF000000 or hex.toLong(16)
        } else {
            hex.toLong(16)
        }
        return Color(argb.toInt())
    }

    return PlayerProgressColorOptions.firstOrNull { it.key == trimmed }?.color ?: Minsk
}

fun resolvePlayerProgressColorOrNull(key: String): Color? {
    val trimmed = key.trim()
    if (trimmed.isEmpty()) {
        return null
    }
    val normalizedHex = normalizeHexColor(trimmed)
    if (normalizedHex != null) {
        val hex = normalizedHex.removePrefix("#")
        val argb = if (hex.length == 6) {
            0xFF000000 or hex.toLong(16)
        } else {
            hex.toLong(16)
        }
        return Color(argb.toInt())
    }

    return PlayerProgressColorOptions.firstOrNull { it.key == trimmed }?.color
}

fun resolvePlayerProgressColorLabel(key: String): String {
    val trimmed = key.trim()
    val normalizedHex = normalizeHexColor(trimmed)
    if (normalizedHex != null) {
        return normalizedHex
    }

    return PlayerProgressColorOptions.firstOrNull { it.key == trimmed }?.label ?: "Purple"
}

fun normalizeHexColor(value: String): String? {
    if (value.isBlank()) {
        return null
    }
    val trimmed = value.trim()
    if (!hexColorRegex.matches(trimmed)) {
        return null
    }
    val hex = trimmed.removePrefix("#").uppercase()
    return "#$hex"
}
