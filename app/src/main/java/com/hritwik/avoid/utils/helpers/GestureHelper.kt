package com.hritwik.avoid.utils.helpers

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterialApi::class)
object GestureHelper {
    fun Modifier.swipeBack(onBack: () -> Unit): Modifier = pointerInput(Unit) {
        detectHorizontalDragGestures { change, dragAmount ->
            if (dragAmount > 100f) {
                onBack()
                change.consume()
            }
        }
    }

    @Composable
    fun rememberPullRefreshState(
        refreshing: Boolean,
        onRefresh: () -> Unit
    ): PullRefreshState =
        androidx.compose.material.pullrefresh.rememberPullRefreshState(refreshing, onRefresh)

    fun Modifier.pullToRefresh(state: PullRefreshState): Modifier = pullRefresh(state)
}
