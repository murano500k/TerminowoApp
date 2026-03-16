package com.stc.terminowo.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PulpitIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pulpit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF49454F))) {
            moveTo(5f, 21f)
            curveTo(4.45f, 21f, 3.975f, 20.8083f, 3.575f, 20.425f)
            curveTo(3.19167f, 20.025f, 3f, 19.55f, 3f, 19f)
            verticalLineTo(5f)
            curveTo(3f, 4.45f, 3.19167f, 3.98333f, 3.575f, 3.6f)
            curveTo(3.975f, 3.2f, 4.45f, 3f, 5f, 3f)
            horizontalLineTo(19f)
            curveTo(19.55f, 3f, 20.0167f, 3.2f, 20.4f, 3.6f)
            curveTo(20.8f, 3.98333f, 21f, 4.45f, 21f, 5f)
            verticalLineTo(19f)
            curveTo(21f, 19.55f, 20.8f, 20.025f, 20.4f, 20.425f)
            curveTo(20.0167f, 20.8083f, 19.55f, 21f, 19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 19f)
            horizontalLineTo(19f)
            verticalLineTo(16f)
            horizontalLineTo(16f)
            curveTo(15.5f, 16.6333f, 14.9f, 17.125f, 14.2f, 17.475f)
            curveTo(13.5167f, 17.825f, 12.7833f, 18f, 12f, 18f)
            curveTo(11.2167f, 18f, 10.475f, 17.825f, 9.775f, 17.475f)
            curveTo(9.09167f, 17.125f, 8.5f, 16.6333f, 8f, 16f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            close()
            moveTo(12f, 16f)
            curveTo(12.6333f, 16f, 13.2083f, 15.8167f, 13.725f, 15.45f)
            curveTo(14.2417f, 15.0833f, 14.6f, 14.6f, 14.8f, 14f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(14f)
            horizontalLineTo(9.2f)
            curveTo(9.4f, 14.6f, 9.75833f, 15.0833f, 10.275f, 15.45f)
            curveTo(10.7917f, 15.8167f, 11.3667f, 16f, 12f, 16f)
            close()
        }
    }.build()
}
