package com.hritwik.avoid.utils.helpers

import com.hritwik.avoid.domain.model.library.MediaItem
import java.text.Normalizer

/**
 * Helper object for alpha scroll functionality.
 * Provides utilities for character normalization and building sectioned grid items.
 */
object AlphaScrollHelper {

    /**
     * Normalizes a character for alpha scrolling.
     * - Converts unicode characters to ASCII equivalents (Å→A, Ñ→N, etc.)
     * - Groups digits and special characters under '#'
     * - Handles null/empty gracefully
     */
    fun normalizeChar(char: Char?): Char {
        if (char == null) return '#'

        // Handle digits - group under '#'
        if (char.isDigit()) return '#'

        // Handle letters - normalize unicode/accents
        if (char.isLetter()) {
            // Normalize unicode (Å→A, Ñ→N, Ö→O, etc.)
            val normalized = Normalizer.normalize(
                char.toString(),
                Normalizer.Form.NFD
            ).replace("\\p{M}".toRegex(), "")
                .uppercase()
                .firstOrNull()

            return if (normalized != null && normalized in 'A'..'Z') {
                normalized
            } else {
                '#'
            }
        }

        return '#'  // All special chars, emojis → '#'
    }

    /**
     * Sealed class representing items in the library grid.
     * Can be either a section header or a media item.
     */
    sealed class LibraryGridItem {
        data class Header(val letter: Char, val index: Int = 0) : LibraryGridItem() {
            val id: String = "header_${letter}_$index"
        }
        data class Media(val item: MediaItem) : LibraryGridItem() {
            val id: String = item.id
        }
    }

    /**
     * Builds a sectioned grid with letter headers from a list of media items.
     *
     * @param items List of media items to section
     * @return Pair of sectioned items list and map of letter to header index
     */
    fun buildSectionedGridItems(items: List<MediaItem>): Pair<List<LibraryGridItem>, Map<Char, Int>> {
        if (items.isEmpty()) {
            return Pair(emptyList(), emptyMap())
        }

        // CRITICAL: Sort items alphabetically first
        val sortedItems = items.sortedBy { item ->
            val name = item.name.trim()
            Normalizer.normalize(name, Normalizer.Form.NFD)
                .replace("\\p{M}".toRegex(), "")
                .lowercase()
        }

        val sectionedItems = mutableListOf<LibraryGridItem>()
        val headerIndices = mutableMapOf<Char, Int>()
        var headerCount = 0

        var currentLetter: Char? = null

        sortedItems.forEach { item ->
            val firstChar = item.name.trim().firstOrNull()
            val letter = normalizeChar(firstChar)

            // Insert header when letter changes
            if (letter != currentLetter) {
                val headerIndex = sectionedItems.size
                sectionedItems.add(LibraryGridItem.Header(letter, headerCount))
                headerIndices[letter] = headerIndex
                currentLetter = letter
                headerCount++
            }

            sectionedItems.add(LibraryGridItem.Media(item))
        }

        return Pair(sectionedItems, headerIndices)
    }

    /**
     * Gets the set of available letters from a list of media items.
     * Used to grey out unavailable letters in the alpha scroller.
     */
    fun getAvailableLetters(items: List<MediaItem>): Set<Char> {
        return items
            .map { item ->
                val firstChar = item.name.trim().firstOrNull()
                normalizeChar(firstChar)
            }
            .toSet()
    }
}
