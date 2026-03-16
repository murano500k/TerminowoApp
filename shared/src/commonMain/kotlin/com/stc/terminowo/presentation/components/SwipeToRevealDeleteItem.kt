package com.stc.terminowo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SwipeToRevealDeleteItem(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val revealWidthDp = 72.dp
    val density = LocalDensity.current
    val revealWidthPx = with(density) { revealWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val accentRed = LocalExtendedColors.current.accentRed

    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) {
        // Delete button behind
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(accentRed),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = {
                    scope.launch { offsetX.animateTo(0f) }
                    onDeleteClick()
                },
                modifier = Modifier.width(revealWidthDp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        // Foreground content
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -revealWidthPx / 2) {
                                    offsetX.animateTo(-revealWidthPx)
                                } else {
                                    offsetX.animateTo(0f)
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(-revealWidthPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
