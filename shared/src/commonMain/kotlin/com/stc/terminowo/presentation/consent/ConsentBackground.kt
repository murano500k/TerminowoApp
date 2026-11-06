package com.stc.terminowo.presentation.consent

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import terminowo.shared.generated.resources.Res

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

    // Advance only at the composition's own frame rate: playing it at the display rate (120 Hz on
    // ProMotion) re-uploads all 80+ image layers every refresh and trips iOS CPU limits.
    val progress = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(composition) {
        val c = composition ?: return@LaunchedEffect
        val totalFrames = c.durationFrames
        if (totalFrames <= 0f) return@LaunchedEffect
        val startNanos = withFrameNanos { it }
        val frameMillis = (1000 / c.frameRate).toLong()
        while (true) {
            withFrameNanos { now ->
                val frame = ((now - startNanos) / 1_000_000_000.0 * c.frameRate).toLong()
                progress.floatValue = (frame % totalFrames.toLong()) / totalFrames
            }
            delay(frameMillis)
        }
    }

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            progress = { progress.floatValue }
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
