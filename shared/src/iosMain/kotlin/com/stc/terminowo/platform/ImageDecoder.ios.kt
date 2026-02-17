package com.stc.terminowo.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
