package com.hritwik.avoid.domain.mapper

import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.extensions.formatRuntime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaMapper @Inject constructor() {

    
    fun calculateProgressPercentage(item: MediaItem): Float {
        val userData = item.userData
        val runTimeTicks = item.runTimeTicks

        return if (userData != null && runTimeTicks != null && runTimeTicks > 0) {
            (userData.playbackPositionTicks.toFloat() / runTimeTicks.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    
    fun getFormattedRuntime(item: MediaItem): String? {
        return item.runTimeTicks?.formatRuntime()
    }

    
    fun getYearString(item: MediaItem): String {
        return item.year?.toString() ?: "Unknown"
    }

    
    fun getGenresString(item: MediaItem): String {
        return if (item.genres.isNotEmpty()) {
            item.genres.joinToString(", ")
        } else {
            "Unknown Genre"
        }
    }

    
    fun getRatingString(item: MediaItem): String? {
        return item.communityRating?.let { rating ->
            "★ %.1f".format(rating)
        }
    }

    
    fun hasContinueWatching(item: MediaItem): Boolean {
        val userData = item.userData
        return userData != null &&
                userData.playbackPositionTicks > 0 &&
                !userData.played
    }

    
    fun isNewItem(item: MediaItem, daysThreshold: Int = 7): Boolean {
        
        
        return false
    }

    
    fun getDisplayTitle(item: MediaItem): String {
        return when (item.type.lowercase()) {
            "episode" -> {
                
                item.name
            }
            "series" -> {
                
                if (item.childCount != null && item.childCount > 0) {
                    "${item.name} (${item.childCount} episodes)"
                } else {
                    item.name
                }
            }
            else -> item.name
        }
    }

    
    fun getSubtitleText(item: MediaItem): String? {
        return when (item.type.lowercase()) {
            "movie" -> {
                val parts = mutableListOf<String>()
                item.year?.let { parts.add(it.toString()) }
                if (item.genres.isNotEmpty()) {
                    parts.add(item.genres.first())
                }
                item.runTimeTicks?.let { parts.add(it.formatRuntime()) }
                parts.joinToString(" • ").takeIf { it.isNotEmpty() }
            }
            "series" -> {
                val parts = mutableListOf<String>()
                item.year?.let { parts.add(it.toString()) }
                if (item.genres.isNotEmpty()) {
                    parts.add(item.genres.first())
                }
                item.childCount?.let {
                    parts.add("$it ${if (it == 1) "episode" else "episodes"}")
                }
                parts.joinToString(" • ").takeIf { it.isNotEmpty() }
            }
            "episode" -> {
                val parts = mutableListOf<String>()
                item.year?.let { parts.add(it.toString()) }
                item.runTimeTicks?.let { parts.add(it.formatRuntime()) }
                parts.joinToString(" • ").takeIf { it.isNotEmpty() }
            }
            else -> {
                val parts = mutableListOf<String>()
                item.year?.let { parts.add(it.toString()) }
                if (item.genres.isNotEmpty()) {
                    parts.add(item.genres.first())
                }
                parts.joinToString(" • ").takeIf { it.isNotEmpty() }
            }
        }
    }

    
    fun getBestImageTag(item: MediaItem, preferBackdrop: Boolean = false): String? {
        return if (preferBackdrop && item.backdropImageTags.isNotEmpty()) {
            item.backdropImageTags.first()
        } else {
            item.primaryImageTag ?: item.backdropImageTags.firstOrNull()
        }
    }

    
    fun groupItemsByFirstLetter(items: List<MediaItem>): Map<String, List<MediaItem>> {
        return items.groupBy { item ->
            item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }
    }

    
    fun filterItemsByQuery(items: List<MediaItem>, query: String): List<MediaItem> {
        if (query.isBlank()) return items

        val lowercaseQuery = query.lowercase()
        return items.filter { item ->
            item.name.lowercase().contains(lowercaseQuery) ||
                    item.overview?.lowercase()?.contains(lowercaseQuery) == true ||
                    item.genres.any { it.lowercase().contains(lowercaseQuery) }
        }
    }

    
    fun sortItemsByRelevance(items: List<MediaItem>, query: String): List<MediaItem> {
        if (query.isBlank()) return items

        val lowercaseQuery = query.lowercase()
        return items.sortedWith(compareByDescending<MediaItem> { item ->
            
            if (item.name.lowercase() == lowercaseQuery) 3
            
            else if (item.name.lowercase().startsWith(lowercaseQuery)) 2
            
            else if (item.name.lowercase().contains(lowercaseQuery)) 1
            
            else 0
        }.thenBy { it.name })
    }
}