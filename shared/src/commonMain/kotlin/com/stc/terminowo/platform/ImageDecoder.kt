package com.stc.terminowo.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
