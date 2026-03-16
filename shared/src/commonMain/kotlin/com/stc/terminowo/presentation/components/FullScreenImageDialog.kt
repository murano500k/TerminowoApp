package com.stc.terminowo.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.decodeImageBitmap
import com.stc.terminowo.platform.getPdfPageCount
import com.stc.terminowo.platform.renderPdfPage
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.close
import terminowo.shared.generated.resources.document_image

@Composable
internal fun FullScreenImageDialog(
    imagePath: String,
    imageStorage: ImageStorage,
    onDismiss: () -> Unit
) {
    val isPdf = imagePath.endsWith(".pdf", ignoreCase = true)
    var fullBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pdfPageCount by remember { mutableStateOf(0) }
    var pdfPageBitmaps by remember { mutableStateOf<Map<Int, ImageBitmap>>(emptyMap()) }

    LaunchedEffect(imagePath) {
        val bytes = imageStorage.readImage(imagePath) ?: return@LaunchedEffect
        if (isPdf) {
            pdfBytes = bytes
            pdfPageCount = getPdfPageCount(bytes)
        } else {
            fullBitmap = decodeImageBitmap(bytes)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isPdf && pdfPageCount > 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pdfPageCount) { pageIndex ->
                        val bitmap = pdfPageBitmaps[pageIndex]

                        LaunchedEffect(pageIndex) {
                            if (bitmap == null && pdfBytes != null) {
                                val rendered = renderPdfPage(pdfBytes!!, pageIndex)
                                if (rendered != null) {
                                    val decoded = decodeImageBitmap(rendered)
                                    if (decoded != null) {
                                        pdfPageBitmaps = pdfPageBitmaps + (pageIndex to decoded)
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { mod ->
                                    if (bitmap != null) mod.aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                    else mod.height(500.dp)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            } else {
                fullBitmap?.let { bitmap ->
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(Res.string.document_image),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = Color.White
                )
            }
        }
    }
}
