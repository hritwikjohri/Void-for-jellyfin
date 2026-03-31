package com.hritwik.avoid.presentation.ui.components.cast

import android.view.ContextThemeWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun CastButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaRouteButtonRef by remember { mutableStateOf<MediaRouteButton?>(null) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .size(1.dp)
                .alpha(0f),
            factory = { ctx ->
                val themedContext = ContextThemeWrapper(ctx, R.style.Theme_Void_CastButton)
                MediaRouteButton(themedContext).apply {
                    CastButtonFactory.setUpMediaRouteButton(context, this)
                    mediaRouteButtonRef = this
                }
            }
        )

        Image(
            painter = painterResource(id = R.drawable.cast),
            contentDescription = "Cast",
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    mediaRouteButtonRef?.performClick()
                }
        )
    }
}
