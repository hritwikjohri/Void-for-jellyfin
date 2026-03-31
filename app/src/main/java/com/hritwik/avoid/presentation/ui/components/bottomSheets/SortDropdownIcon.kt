package com.hritwik.avoid.presentation.ui.components.bottomSheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
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
import com.hritwik.avoid.presentation.ui.screen.library.LibrarySortOption
import com.hritwik.avoid.presentation.ui.theme.Minsk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDropdownIcon(
    selectedSort: LibrarySortOption,
    sortOptions: List<LibrarySortOption>,
    onSortSelected: (LibrarySortOption) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    IconButton(
        onClick = { showBottomSheet = true },
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Sort,
            contentDescription = "Sort options",
            tint = Color.White
        )
    }

    if (showBottomSheet) {
        VoidModalSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sortOptions) { option ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = option.label,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortSelected(option)
                                showBottomSheet = false
                            }
                            .padding(horizontal = 4.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        trailingContent = {
                            if (option.id == selectedSort.id) {
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
