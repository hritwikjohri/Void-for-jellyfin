package com.hritwik.avoid.presentation.ui.screen.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.LibraryGridCard
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySection(
    onLibraryClick: (String, String) -> Unit = { _, _ -> },
    authViewModel: AuthServerViewModel,
    onSearchClick: () -> Unit = {},
    libraryViewModel: LibraryViewModel,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()
    val isNetworkAvailable by userDataViewModel.isConnected.collectAsStateWithLifecycle()

    AnimatedAmbientBackground {
        if (!isNetworkAvailable) {
            EmptyState(
                icon = Icons.Default.WifiOff,
                title = "Offline",
                description = "Looks like you don't have a stable internet connection."
            )
        } else {
            if (libraryState.libraries.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(calculateRoundedValue(10).sdp),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                ) {
                    item {
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    }

                    item {
                        SectionHeader(
                            title = "Your Libraries",
                            subtitle = "Explore your saved libraries",
                            actionButton = {
                                IconButton(
                                    onClick = onSearchClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = PrimaryText,
                                        modifier = Modifier.size(calculateRoundedValue(28).sdp)
                                    )
                                }
                            }
                        )
                    }

                    items(libraryState.libraries.chunked(2)) { libraryRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
                        ) {
                            libraryRow.forEach { library ->
                                LibraryGridCard(
                                    library = library,
                                    serverUrl = authState.authSession?.server?.url ?: "",
                                    onClick = { onLibraryClick(it.id, it.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (libraryRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryAdd,
                        contentDescription = "Add LibrarySection",
                        modifier = Modifier.size(calculateRoundedValue(80).sdp)
                    )

                    Spacer(modifier = Modifier.height(calculateRoundedValue(10).sdp))

                    Text(
                        text = "No LibrarySection found!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}