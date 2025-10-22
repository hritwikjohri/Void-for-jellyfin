package com.hritwik.avoid.presentation.ui.components.common

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        modifier = Modifier
            .fillMaxHeight(0.8f)
            
            .padding(top = 16.dp),
        dragHandle = {
            FloatingNotchDragHandle(
                notchWidth = 120.dp,
                notchDepth = 28.dp,
                lipRadius = 22.dp,
                pillWidth = 36.dp,
                pillHeight = 12.dp,
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        
    }
}

@Composable
fun FloatingNotchDragHandlesvds(
    notchWidth: Dp = 120.dp,
    notchDepth: Dp = 28.dp,
    lipRadius: Dp = 20.dp,
    pillWidth: Dp = 36.dp,
    pillHeight: Dp = 12.dp,
    shadowBlur: Dp = 24.dp,
    shadowOffsetY: Dp = 8.dp,
) {
    val surface = MaterialTheme.colorScheme.surface
    val pillColor = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current
    val notchW = with(density) { notchWidth.toPx() }
    val depth = with(density) { notchDepth.toPx() }
    val r = with(density) { lipRadius.toPx() }
    val blur = with(density) { shadowBlur.toPx() }
    val dy = with(density) { shadowOffsetY.toPx() }
    val headerHeight = notchDepth + pillHeight + 24.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .drawBehind {
                val w = size.width
                val h = size.height

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(38, 0, 0, 0) 
                        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
                        isAntiAlias = true
                    }
                    val cx = w / 2f
                    val shadowRectW = notchW * 0.9f
                    val shadowRectH = depth * 1.1f
                    canvas.nativeCanvas.drawOval(
                        cx - shadowRectW / 2f,
                        depth + dy,
                        cx + shadowRectW / 2f,
                        depth + dy + shadowRectH,
                        paint
                    )
                }

                val path = Path().apply {
                    val cx = w / 2f
                    val left = cx - notchW / 2f
                    val right = cx + notchW / 2f

                    moveTo(0f, 0f)
                    lineTo(left - r, 0f)

                    quadraticTo(
                        left, 0f,
                        left + r, depth * 0.55f
                    )

                    quadraticTo(
                        cx, depth + depth * 0.25f,
                        right - r, depth * 0.55f
                    )

                    quadraticTo(
                        right, 0f,
                        right + r, 0f
                    )

                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(path = path, color = surface)
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = notchDepth - pillHeight / 2) 
                .size(width = pillWidth, height = pillHeight)
                .background(pillColor, RoundedCornerShape(50))
        )
    }
}



@Composable
private fun NotchedDragHandle(
    tabWidth: Dp = 68.dp,
    tabHeight: Dp = 30.dp,
    pillWidth: Dp = 28.dp,
    pillHeight: Dp = 10.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(tabHeight / 2 + pillHeight), 
        contentAlignment = Alignment.TopCenter
    ) {
        
        Box(
            modifier = Modifier
                .size(tabWidth, tabHeight)
                .offset(y = -(tabHeight / 2))
                .background(Color.Yellow, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(pillWidth, pillHeight)
                    .background(Color.Red, RoundedCornerShape(50))
            )
        }

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tabHeight / 2)
                .align(Alignment.BottomCenter) 
        )
    }
}







@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    var showSheet by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { showSheet = true }) {
                Text("Open Bottom Sheet")
            }
        }

        MyBottomSheet(
            onDismiss = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        )
    }
}
