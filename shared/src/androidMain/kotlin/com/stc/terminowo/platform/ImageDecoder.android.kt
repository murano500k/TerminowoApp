package com.stc.terminowo.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}
