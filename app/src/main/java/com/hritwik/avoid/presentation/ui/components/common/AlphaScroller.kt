package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.launch

@Composable
fun AlphaScroller(
    items: List<MediaItem>,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    idleHideDelayMillis: Long = 1200,
    onActiveLetterChange: (Char?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val indexMap = remember(items.map { it.id to it.name }) {
        buildMap {
            items.forEachIndexed { index, item ->
                val c = item.name.firstOrNull()?.uppercaseChar() ?: '#'
                putIfAbsent(c, index)
            }
        }
    }
    val letters = remember { ('A'..'Z') + '#' }

    var visible by remember { mutableStateOf(false) }
    var interacting by remember { mutableStateOf(false) }

    LaunchedEffect(gridState.isScrollInProgress, interacting) {
        if (gridState.isScrollInProgress || interacting) {
            visible = true
        } else {
            kotlinx.coroutines.delay(idleHideDelayMillis)
            if (!gridState.isScrollInProgress && !interacting) visible = false
        }
    }

    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    fun yToLetter(y: Float): Char {
        if (size.height <= 0) return letters.last()
        val seg = size.height.toFloat() / letters.size
        val idx = (y / seg).toInt().coerceIn(0, letters.lastIndex)
        return letters[idx]
    }
    fun jumpTo(letter: Char) {
        indexMap[letter]?.let { idx -> scope.launch { gridState.scrollToItem(idx) } }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200))
    ) {
        Column (
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(calculateRoundedValue(24).sdp))
                .onGloballyPositioned { size = it.size }
                .pointerInput(letters, indexMap, size) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        interacting = true
                        val first = yToLetter(down.position.y)
                        onActiveLetterChange(first)
                        jumpTo(first)

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                interacting = false
                                onActiveLetterChange(null)
                                break
                            }
                            val letter = yToLetter(change.position.y)
                            onActiveLetterChange(letter)
                            jumpTo(letter)
                            change.consume()
                        }
                    }
                }
                .clip(RoundedCornerShape(50))
        ) {
            Column(
                modifier = modifier.padding(calculateRoundedValue(4).sdp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                letters.forEach { ch ->
                    Text(
                        text = ch.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

    }
}