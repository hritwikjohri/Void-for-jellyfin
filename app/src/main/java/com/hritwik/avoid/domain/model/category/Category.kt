package com.hritwik.avoid.domain.model.category

import androidx.compose.ui.graphics.Color

data class Category(
    val id: String,
    val name: String,
    val description: String,
    val gradientColors: List<Color>,
    val iconName: String,
    val searchFilters: Map<String, String> = emptyMap()
)