package com.hritwik.avoid.presentation.ui.components.bottomSheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoidModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { target ->
            target != SheetValue.Expanded
        }
    ),
    imageUrl: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        modifier = modifier
    ) {
        AnimatedAmbientBackground(
            imageUrl = imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = calculateRoundedValue(240).sdp)
                .clip(
                    RoundedCornerShape(
                        topStart = calculateRoundedValue(22).sdp,
                        topEnd = calculateRoundedValue(22).sdp
                    )
                )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(calculateRoundedValue(64).sdp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = calculateRoundedValue(16).sdp,
                            vertical = calculateRoundedValue(12).sdp
                        ),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                    content = content
                )
            }
        }
    }
}
