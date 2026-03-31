package com.hritwik.avoid.presentation.ui.components.bottomSheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDropdownIcon(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    onRetry: () -> Unit,
    hasError: Boolean
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    IconButton(
        onClick = {
            if (genres.isNotEmpty()) {
                showBottomSheet = true
            } else if (hasError) {
                onRetry()
            }
        },
        enabled = genres.isNotEmpty() || hasError,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = "Genre filter",
            tint = Color.White
        )
    }

    if (showBottomSheet) {
        VoidModalSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Text(
                text = "Genre",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "All genres",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onGenreSelected(null)
                                showBottomSheet = false
                            }
                            .padding(horizontal = 4.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        trailingContent = {
                            if (selectedGenre == null) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = Minsk,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    )
                }

                items(genres) { genre ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = genre,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onGenreSelected(genre)
                                showBottomSheet = false
                            }
                            .padding(horizontal = 4.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        trailingContent = {
                            if (genre == selectedGenre) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = Minsk,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
