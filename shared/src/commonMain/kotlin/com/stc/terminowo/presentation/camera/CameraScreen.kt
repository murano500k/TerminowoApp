package com.stc.terminowo.presentation.camera

import androidx.compose.runtime.Composable

@Composable
expect fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onBack: () -> Unit
)
