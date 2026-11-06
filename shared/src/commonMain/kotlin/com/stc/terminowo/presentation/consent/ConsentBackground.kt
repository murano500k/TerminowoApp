package com.stc.terminowo.presentation.consent

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import terminowo.shared.generated.resources.Res
import kotlin.math.max

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ConsentBackground() {
    val path = if (isSystemInDarkTheme()) {
        "files/terminowo_tile_dark.json"
    } else {
        "files/terminowo_tile_light.json"
    }

    val composition by rememberLottieComposition(path) {
        LottieCompositionSpec.JsonString(Res.readBytes(path).decodeToString())
    }

    // The animation has 80+ image layers and the Lottie painter keeps invalidating itself, so drawing
    // it directly re-renders every display refresh and trips iOS CPU limits. Instead, render one frame
    // per composition frame into a layer and only replay that layer in between.
    val progress = remember { floatArrayOf(0f) }
    val painter = rememberLottiePainter(composition = composition, progress = { progress[0] })
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var renderedFrames by remember { mutableIntStateOf(0) }

    LaunchedEffect(composition, size, density, layoutDirection) {
        val c = composition ?: return@LaunchedEffect
        val totalFrames = c.durationFrames
        if (totalFrames < 1f || c.frameRate <= 0f || size == IntSize.Zero) return@LaunchedEffect
        val frameMillis = (1000 / c.frameRate).toLong()
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val frame = ((now - startNanos) / 1_000_000_000.0 * c.frameRate).toLong()
                progress[0] = (frame % totalFrames.toLong()) / totalFrames
                val intrinsic = painter.intrinsicSize
                if (intrinsic.isUnspecified || intrinsic.width <= 0f || intrinsic.height <= 0f) return@withFrameNanos
                layer.record(density, layoutDirection, size) {
                    val scale = max(this.size.width / intrinsic.width, this.size.height / intrinsic.height)
                    translate(
                        left = (this.size.width - intrinsic.width * scale) / 2,
                        top = (this.size.height - intrinsic.height * scale) / 2
                    ) {
                        scale(scale, pivot = Offset.Zero) {
                            with(painter) { draw(Size(intrinsic.width, intrinsic.height)) }
                        }
                    }
                }
                renderedFrames++
            }
            delay(frameMillis)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .drawBehind {
                if (renderedFrames > 0) drawLayer(layer)
            }
    )
}
