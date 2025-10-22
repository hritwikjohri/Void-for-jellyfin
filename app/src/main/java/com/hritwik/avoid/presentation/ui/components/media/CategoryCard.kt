package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.category.Category

@Composable
fun CategoryCard(
    category: Category,
    modifier: Modifier = Modifier,
    onClick: (Category) -> Unit = {}
) {
    Card(
        modifier = modifier
            .wrapContentSize()
            .clickable { onClick(category) },
        elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(6).sdp),
        shape = RoundedCornerShape(calculateRoundedValue(16).sdp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = category.gradientColors
                    )
                )
                .clip(RoundedCornerShape(calculateRoundedValue(16).sdp))
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            radius = 200f
                        )
                    )
            )

            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(calculateRoundedValue(16).sdp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = category.name,
                    modifier = Modifier.size(calculateRoundedValue(32).sdp),
                    tint = Color.White.copy(alpha = 0.9f)
                )

                
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "movie" -> Icons.Default.Movie
        "tv" -> Icons.Default.Tv
        "anime" -> Icons.Default.AutoStories
        "documentary" -> Icons.Default.School
        "action" -> Icons.Default.Whatshot
        "comedy" -> Icons.Default.SentimentSatisfied
        "horror" -> Icons.Default.Dangerous
        "sci_fi" -> Icons.Default.Psychology
        else -> Icons.Default.Movie
    }
}