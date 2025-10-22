package com.hritwik.avoid.presentation.ui.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    onClear: () -> Unit = {},
    onClearFocus: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    showJellyseerToggle: Boolean = false,
    isJellyseerActive: Boolean = false,
    isJellyseerToggleEnabled: Boolean = true,
    onJellyseerToggle: () -> Unit = {},
    @androidx.annotation.StringRes placeholderRes: Int = R.string.search_placeholder
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val placeholder = stringResource(placeholderRes)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search)
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showJellyseerToggle) {
                    IconToggleButton(
                        checked = isJellyseerActive,
                        onCheckedChange = { onJellyseerToggle() },
                        enabled = isJellyseerToggleEnabled,
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        val tint = if (isJellyseerActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Toggle Jellyseerr search",
                            tint = if (isJellyseerToggleEnabled) tint else tint.copy(alpha = 0.4f)
                        )
                    }
                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(calculateRoundedValue(4).sdp))
                    }
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onClear()
                            onClearFocus()
                        },
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (query.trim().isNotEmpty()) {
                    onSearch(query.trim())
                    onClearFocus()
                }
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                val newFocusState = focusState.isFocused
                if (isFocused != newFocusState) {
                    isFocused = newFocusState
                    onFocusChange(newFocusState)
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                
            },
        singleLine = true,
        shape = RoundedCornerShape(calculateRoundedValue(28).sdp)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSearches(
    modifier: Modifier = Modifier,
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    onClearAll: () -> Unit = {}
) {
    if (recentSearches.isEmpty()) return

    var showAllSearches by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = stringResource(R.string.clear_all),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onClearAll() }
            )
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

        
        Column(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            
            recentSearches.take(5).forEach { search ->
                RecentSearchItem(
                    search = search,
                    onSearchClick = onSearchClick
                )
            }

            
            if (recentSearches.size > 5) {
                TextButton(
                    onClick = { showAllSearches = true },
                    modifier = Modifier.padding(top = calculateRoundedValue(4).sdp)
                ) {
                    Text(stringResource(R.string.view_more, recentSearches.size - 5))
                }
            }
        }
    }

    
    if (showAllSearches) {
        ModalBottomSheet(
            onDismissRequest = { showAllSearches = false },
            sheetState = sheetState
        ) {
            AllRecentSearchesContent(
                recentSearches = recentSearches,
                onSearchClick = { search ->
                    onSearchClick(search)
                    showAllSearches = false
                },
                onClearAll = {
                    onClearAll()
                    showAllSearches = false
                },
                onDismiss = { showAllSearches = false }
            )
        }
    }
}

@Composable
fun RecentSearchItem(
    search: String,
    onSearchClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSearchClick(search) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(calculateRoundedValue(12).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(calculateRoundedValue(20).sdp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

            Text(
                text = search,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AllRecentSearchesContent(
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(calculateRoundedValue(16).sdp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.all_recent_searches),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = onClearAll) {
                Text(stringResource(R.string.clear_all))
            }
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp),
            modifier = Modifier.height(calculateRoundedValue(400).sdp)
        ) {
            items(recentSearches.take(24)) { search ->
                RecentSearchItem(
                    search = search,
                    onSearchClick = onSearchClick
                )
            }
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.done))
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
    }
}