package com.hritwik.avoid.utils.helpers

import kotlin.math.max

data class SemanticVersion(private val components: List<Int>) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        val maxSize = max(components.size, other.components.size)
        for (index in 0 until maxSize) {
            val left = components.getOrElse(index) { 0 }
            val right = other.components.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    companion object {
        private const val MAX_COMPONENTS = 3

        fun parse(raw: String?): SemanticVersion? {
            if (raw.isNullOrBlank()) return null
            val sanitized = raw.trim().removePrefix("v")
            val parts = sanitized
                .split(Regex("[^0-9]+"))
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
                .take(MAX_COMPONENTS)
            if (parts.isEmpty()) return null
            return SemanticVersion(parts)
        }

        fun of(major: Int, minor: Int, patch: Int = 0): SemanticVersion {
            return SemanticVersion(listOf(major, minor, patch))
        }
    }
}
