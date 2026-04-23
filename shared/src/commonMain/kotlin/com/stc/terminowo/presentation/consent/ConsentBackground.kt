package com.stc.terminowo.presentation.consent

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
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

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            iterations = Compottie.IterateForever
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
