package com.stc.terminowo.presentation.preview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.usecase.ScanDocumentUseCase
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.decodeImageBitmap
import com.stc.terminowo.platform.getPdfPageCount
import com.stc.terminowo.platform.renderPdfPage
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.add_document
import terminowo.shared.generated.resources.cancel
import terminowo.shared.generated.resources.captured_document
import terminowo.shared.generated.resources.close
import terminowo.shared.generated.resources.extracting_expiry_date
import terminowo.shared.generated.resources.failed_load_image
import terminowo.shared.generated.resources.failed_read_image
import terminowo.shared.generated.resources.get_expiry_date
import terminowo.shared.generated.resources.ocr_processing_failed
import terminowo.shared.generated.resources.retake
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ImagePreviewScreen(
    imagePath: String,
    mimeType: String = "image/jpeg",
    onRetake: () -> Unit,
    onScanResult: (
        name: String?,
        expiryDate: String?,
        confidence: Float?,
        imagePath: String,
        thumbnailPath: String,
        rawOcrResponse: String?,
        documentId: String,
        category: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    val scanDocumentUseCase: ScanDocumentUseCase = koinInject()
    val imageStorage: ImageStorage = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val accentRed = LocalExtendedColors.current.accentRed

    var isProcessing by remember { mutableStateOf(false) }
    val isPdf = mimeType == "application/pdf"

    // Image state (for non-PDF)
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }

    // PDF state
    var pdfPageCount by remember { mutableStateOf(0) }
    var pdfPageBitmaps by remember { mutableStateOf<Map<Int, ImageBitmap>>(emptyMap()) }
    var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(imagePath, mimeType) {
        val bytes = imageStorage.readImage(imagePath)
        if (bytes == null) {
            imageLoadError = true
            return@LaunchedEffect
        }

        if (isPdf) {
            pdfBytes = bytes
            pdfPageCount = getPdfPageCount(bytes)
            if (pdfPageCount == 0) imageLoadError = true
        } else {
            imageBitmap = decodeImageBitmap(bytes)
            if (imageBitmap == null) imageLoadError = true
        }
    }

    fun startOcrProcessing() {
        isProcessing = true
        scope.launch {
            val failedReadMsg = getString(Res.string.failed_read_image)

            val imageBytes = imageStorage.readImage(imagePath)
            if (imageBytes == null) {
                snackbarHostState.showSnackbar(failedReadMsg)
                isProcessing = false
                return@launch
            }

            val result = scanDocumentUseCase(imageBytes, mimeType)
            val ocrFailedMsg = getString(Res.string.ocr_processing_failed)
            result.fold(
                onSuccess = { scanResult ->
                    val docId = Uuid.random().toString()

                    // For PDFs, render first page as thumbnail source
                    val thumbnailSourceBytes = if (isPdf) {
                        renderPdfPage(imageBytes, 0) ?: imageBytes
                    } else {
                        imageBytes
                    }

                    val thumbnailPath = imageStorage.saveThumbnail(
                        thumbnailSourceBytes,
                        "$docId.jpg"
                    )
                    onScanResult(
                        scanResult.extractedName,
                        scanResult.expiryDate?.toString(),
                        scanResult.confidence,
                        imagePath,
                        thumbnailPath,
                        scanResult.rawResponse,
                        docId,
                        scanResult.detectedCategory?.key
                    )
                },
                onFailure = {
                    isProcessing = false
                    snackbarHostState.showSnackbar(ocrFailedMsg)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.add_document)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.close)
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image preview area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    imageLoadError -> {
                        Text(
                            text = stringResource(Res.string.failed_load_image),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    isPdf && pdfPageCount > 0 -> {
                        PdfPagesPreview(
                            pdfBytes = pdfBytes,
                            pageCount = pdfPageCount,
                            pageBitmaps = pdfPageBitmaps,
                            onPageRendered = { index, bitmap ->
                                pdfPageBitmaps = pdfPageBitmaps + (index to bitmap)
                            }
                        )
                    }
                    imageBitmap != null -> {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = stringResource(Res.string.captured_document),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        CircularProgressIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                // Processing state: spinner + text + cancel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArcProgressIndicator(color = accentRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.extracting_expiry_date),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { isProcessing = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            } else {
                // Action buttons: Retake + Get expiry date
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(stringResource(Res.string.retake))
                    }

                    Button(
                        onClick = { startOcrProcessing() },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentRed,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(Res.string.get_expiry_date))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Spinning arc progress indicator matching the Figma design. */
@Composable
private fun ArcProgressIndicator(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.size(24.dp)) {
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PdfPagesPreview(
    pdfBytes: ByteArray?,
    pageCount: Int,
    pageBitmaps: Map<Int, ImageBitmap>,
    onPageRendered: (Int, ImageBitmap) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pageCount) { pageIndex ->
            val bitmap = pageBitmaps[pageIndex]

            LaunchedEffect(pageIndex) {
                if (bitmap == null && pdfBytes != null) {
                    val rendered = renderPdfPage(pdfBytes, pageIndex)
                    if (rendered != null) {
                        val decoded = decodeImageBitmap(rendered)
                        if (decoded != null) {
                            onPageRendered(pageIndex, decoded)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { mod ->
                        if (bitmap != null) mod.aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                        else mod.height(400.dp)
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
                    CircularProgressIndicator()
                }
            }
        }
    }
}
