package com.hritwik.avoid.presentation.ui.screen.library

import com.hritwik.avoid.domain.model.library.LibrarySortDirection

data class LibrarySortOption(
    val id: String,
    val label: String,
    val sortFields: List<String>,
    val defaultDirection: LibrarySortDirection,
    val allowDirectionToggle: Boolean = true,
    val supportsAlphaScroller: Boolean = false
)

object LibrarySortOptions {
    val SortName = LibrarySortOption(
        id = "sort_name",
        label = "Title (A-Z)",
        sortFields = listOf("SortName"),
        defaultDirection = LibrarySortDirection.ASCENDING,
        supportsAlphaScroller = true
    )

    val Name = LibrarySortOption(
        id = "name",
        label = "Name",
        sortFields = listOf("Name"),
        defaultDirection = LibrarySortDirection.ASCENDING,
        supportsAlphaScroller = true
    )

    val DateAdded = LibrarySortOption(
        id = "date_added",
        label = "Recently added",
        sortFields = listOf("DateCreated", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val PremiereDate = LibrarySortOption(
        id = "premiere_date",
        label = "Premiere date",
        sortFields = listOf("PremiereDate", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val ProductionYear = LibrarySortOption(
        id = "production_year",
        label = "Year",
        sortFields = listOf("ProductionYear", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val Runtime = LibrarySortOption(
        id = "runtime",
        label = "Runtime",
        sortFields = listOf("Runtime", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val CommunityRating = LibrarySortOption(
        id = "community_rating",
        label = "Community rating",
        sortFields = listOf("CommunityRating", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val CriticRating = LibrarySortOption(
        id = "critic_rating",
        label = "Critic rating",
        sortFields = listOf("CriticRating", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val DatePlayed = LibrarySortOption(
        id = "date_played",
        label = "Last played",
        sortFields = listOf("DatePlayed", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val PlayCount = LibrarySortOption(
        id = "play_count",
        label = "Most played",
        sortFields = listOf("PlayCount", "SortName"),
        defaultDirection = LibrarySortDirection.DESCENDING
    )

    val Random = LibrarySortOption(
        id = "random",
        label = "Random",
        sortFields = listOf("Random"),
        defaultDirection = LibrarySortDirection.ASCENDING,
        allowDirectionToggle = false
    )

    val All: List<LibrarySortOption> = listOf(
        SortName,
        Name,
        DateAdded,
        PremiereDate,
        ProductionYear,
        Runtime,
        CommunityRating,
        CriticRating,
        DatePlayed,
        PlayCount,
        Random
    )
}
