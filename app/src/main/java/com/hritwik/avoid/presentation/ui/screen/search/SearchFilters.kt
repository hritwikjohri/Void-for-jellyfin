package com.hritwik.avoid.presentation.ui.screen.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.presentation.ui.state.SearchFilters
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
internal fun SearchFilterDropdown(
    filters: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var pendingFilters by remember { mutableStateOf(filters) }

    LaunchedEffect(filters, expanded) {
        if (!expanded) {
            pendingFilters = filters
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                pendingFilters = filters
                expanded = !expanded
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = "Filter results",
                tint = PrimaryText
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                pendingFilters = filters
            },
            shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
            shadowElevation = 0.dp,
            containerColor = Color.Black.copy(alpha = 0.8f),
            border = BorderStroke(
                width = calculateRoundedValue(2).sdp,
                color = Minsk
            )
        ) {
            Text(
                text = "Filters",
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                textAlign = TextAlign.Center,
                modifier = modifier.fillMaxWidth().padding(top = calculateRoundedValue(4).sdp)
            )
            FilterDropdownOption(
                label = "Movies",
                checked = pendingFilters.includeMovies,
                onCheckedChange = { isChecked ->
                    pendingFilters = pendingFilters.copy(includeMovies = isChecked)
                }
            )

            FilterDropdownOption(
                label = "TV Shows",
                checked = pendingFilters.includeTvShows,
                onCheckedChange = { isChecked ->
                    pendingFilters = pendingFilters.copy(includeTvShows = isChecked)
                }
            )

            FilterDropdownOption(
                label = "Episodes",
                checked = pendingFilters.includeEpisodes,
                onCheckedChange = { isChecked ->
                    pendingFilters = pendingFilters.copy(includeEpisodes = isChecked)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = calculateRoundedValue(12).sdp,
                        vertical = calculateRoundedValue(8).sdp
                    ),
                horizontalArrangement = Arrangement.spacedBy(
                    calculateRoundedValue(8).sdp,
                    Alignment.End
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onApply(pendingFilters)
                        expanded = false
                    }
                ) {
                    Text("Apply")
                }
                TextButton(
                    onClick = {
                        pendingFilters = pendingFilters.copy(
                            includeMovies = false,
                            includeTvShows = false,
                            includeEpisodes = false
                        )
                    }
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(2).sdp)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
                Text(text = label)
            }
        },
        onClick = {
            onCheckedChange(!checked)
        }
    )
}
