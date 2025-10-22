package com.hritwik.avoid.presentation.ui.components.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import com.hritwik.avoid.R
import ir.kaaveh.sdpcompose.sdp

@Composable
fun BottomBar(
    selectedItem: Int = 0,
    onItemSelected: (Int) -> Unit = {}
) {
    val bottomNavItems = listOf(
        BottomNavItem(
            label = R.string.bottom_nav_home,
            selectedIcon = Icons.Outlined.Home,
            route = "home",
            selectedDrawable = R.drawable.home
        ),
        BottomNavItem(
            label = R.string.bottom_nav_library,
            selectedIcon = Icons.Outlined.VideoLibrary,
            route = "library",
            selectedDrawable = R.drawable.library
        ),
        BottomNavItem(
            label = R.string.bottom_nav_profile,
            selectedIcon = Icons.Outlined.Person,
            route = "profile",
            selectedDrawable = R.drawable.void_personalize
        )
    )

    val borderBrush = remember(selectedItem) { createShiningBorderBrush(selectedItem) }
    val backgroundGradient = remember(selectedItem) { createDynamicGradient(selectedItem) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = (calculateRoundedValue(400).sdp))
                .fillMaxWidth()
                .padding(horizontal = (calculateRoundedValue(74).sdp))
                .height(calculateRoundedValue(80).sdp)
                .zIndex(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                )
                .background(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(calculateRoundedValue(40).sdp)
                )
                .border(
                    width = calculateRoundedValue(2).sdp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(calculateRoundedValue(40).sdp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = backgroundGradient,
                        shape = RoundedCornerShape(calculateRoundedValue(40).sdp)
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    BottomNavButton(
                        item = item,
                        isSelected = selectedItem == index,
                        onClick = { onItemSelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(200),
        label = "scale_animation"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(200),
        label = "alpha_animation"
    )

    val buttonFocusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .size(calculateRoundedValue(48).sdp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick() }
            )
            .focusRequester(buttonFocusRequester)
            .semantics {
                role = Role.Tab
                selected = isSelected
                onClick { onClick(); true }
            }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(calculateRoundedValue(24).sdp)
                .scale(animatedScale),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Image(
                    painter = painterResource(id = item.selectedDrawable),
                    contentDescription = stringResource(item.label),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = item.selectedIcon,
                    contentDescription = stringResource(item.label),
                    tint = Color.White.copy(alpha = animatedAlpha),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun createShiningBorderBrush(selectedIndex: Int): Brush {
    return when (selectedIndex) {
        0 -> Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.1f)
            )
        )
        1 -> Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.09f),
                Color.White.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.1f)
            )
        )
        2 -> Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.6f)
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Black.copy(alpha = 0.1f)
            )
        )
    }
}

private fun createDynamicGradient(selectedIndex: Int): Brush {
    return when (selectedIndex) {
        0 -> Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.1f)
            )
        )
        1 -> Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.25f),
                Color.Black.copy(alpha = 0.1f)
            )
        )
        2 -> Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.25f)
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Black.copy(alpha = 0.1f)
            )
        )
    }
}