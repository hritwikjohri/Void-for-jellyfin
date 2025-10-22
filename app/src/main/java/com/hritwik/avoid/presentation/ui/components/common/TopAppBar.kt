package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.theme.BlackPearl
import com.hritwik.avoid.presentation.ui.theme.PrimaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onSearchClick: () -> Unit) {
    val searchFocusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            NetworkImage(
                data = R.drawable.void_icon,
                contentDescription = stringResource(R.string.app_logo_desc),
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .semantics { role = Role.Image },
                contentScale = ContentScale.Fit
            )
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .focusRequester(searchFocusRequester)
                    .semantics { role = Role.Button }
                    .focusable()
            ) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
            }
        },
        modifier = Modifier.wrapContentHeight(),
        colors = TopAppBarColors(
            containerColor = BlackPearl,
            scrolledContainerColor = BlackPearl,
            navigationIconContentColor = BlackPearl,
            titleContentColor = BlackPearl,
            actionIconContentColor = PrimaryText
        )
    )

}