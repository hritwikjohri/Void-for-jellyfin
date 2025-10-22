package com.hritwik.avoid.domain.model.category

import androidx.compose.ui.graphics.Color

object CategoryProvider {
    fun getCategories(): List<Category> = listOf(
        Category(
            id = "movies",
            name = "Movies",
            description = "Feature films and cinema",
            gradientColors = listOf(
                Color(0xFF1976D2),
                Color(0xFF42A5F5)
            ),
            iconName = "movie",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "tv_shows",
            name = "TV Shows",
            description = "Television series and episodes",
            gradientColors = listOf(
                Color(0xFF7B1FA2),
                Color(0xFFAB47BC)
            ),
            iconName = "tv",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Series",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "anime",
            name = "Anime",
            description = "Japanese animated content",
            gradientColors = listOf(
                Color(0xFFE91E63),
                Color(0xFFFF6B9D)
            ),
            iconName = "anime",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie,Series",
                "Genres" to "Anime",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "documentary",
            name = "Documentary",
            description = "Educational and factual content",
            gradientColors = listOf(
                Color(0xFF388E3C),
                Color(0xFF66BB6A)
            ),
            iconName = "documentary",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie,Series",
                "Genres" to "Documentary",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "action",
            name = "Action",
            description = "High-energy adventure content",
            gradientColors = listOf(
                Color(0xFFD32F2F),
                Color(0xFFEF5350)
            ),
            iconName = "action",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie,Series",
                "Genres" to "Action",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "comedy",
            name = "Comedy",
            description = "Humorous and entertaining content",
            gradientColors = listOf(
                Color(0xFFFF9800),
                Color(0xFFFFB74D)
            ),
            iconName = "comedy",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie,Series",
                "Genres" to "Comedy",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "horror",
            name = "Horror",
            description = "Scary and suspenseful content",
            gradientColors = listOf(
                Color(0xFF424242),
                Color(0xFF757575)
            ),
            iconName = "horror",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie,Series",
                "Genres" to "Horror",
                "Recursive" to "true"
            )
        ),
        Category(
            id = "sci_fi",
            name = "Sci-Fi",
            description = "Science fiction and futuristic content",
            gradientColors = listOf(
                Color(0xFF00BCD4),
                Color(0xFF4DD0E1)
            ),
            iconName = "sci_fi",
            searchFilters = mapOf(
                "IncludeItemTypes" to "Movie,Series",
                "Genres" to "Science Fiction",
                "Recursive" to "true"
            )
        )
    )
}