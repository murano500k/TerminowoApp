package com.stc.terminowo.presentation.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.stc.terminowo.domain.model.ScanResult
import com.stc.terminowo.domain.usecase.ScanDocumentUseCase
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.decodeImageBitmap
import com.stc.terminowo.presentation.components.LoadingOverlay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.back
import terminowo.shared.generated.resources.captured_document
import terminowo.shared.generated.resources.extracting_expiry_date
import terminowo.shared.generated.resources.failed_load_image
import terminowo.shared.generated.resources.failed_read_image
import terminowo.shared.generated.resources.get_expiry_date
import terminowo.shared.generated.resources.ocr_processing_failed
import terminowo.shared.generated.resources.retake
import terminowo.shared.generated.resources.review_image
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ImagePreviewScreen(
    imagePath: String,
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

    var isProcessing by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }

    LaunchedEffect(imagePath) {
        val bytes = imageStorage.readImage(imagePath)
        if (bytes != null) {
            imageBitmap = decodeImageBitmap(bytes)
            if (imageBitmap == null) imageLoadError = true
        } else {
            imageLoadError = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.review_image)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                        imageBitmap != null -> {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = stringResource(Res.string.captured_document),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        imageLoadError -> {
                            Text(
                                text = stringResource(Res.string.failed_load_image),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            CircularProgressIndicator()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text(stringResource(Res.string.retake))
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                val failedReadMsg = getString(Res.string.failed_read_image)
                                val ocrFailedMsg = getString(Res.string.ocr_processing_failed)

                                val imageBytes = imageStorage.readImage(imagePath)
                                if (imageBytes == null) {
                                    snackbarHostState.showSnackbar(failedReadMsg)
                                    isProcessing = false
                                    return@launch
                                }

                                val result = scanDocumentUseCase(imageBytes, "image/jpeg")
                                result.fold(
                                    onSuccess = { scanResult ->
                                        val docId = Uuid.random().toString()
                                        val thumbnailPath = imageStorage.saveThumbnail(
                                            imageBytes,
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
                                    onFailure = { error ->
                                        val docId = Uuid.random().toString()
                                        val thumbnailPath = imageStorage.saveThumbnail(
                                            imageBytes,
                                            "$docId.jpg"
                                        )
                                        onScanResult(
                                            null,
                                            null,
                                            null,
                                            imagePath,
                                            thumbnailPath,
                                            null,
                                            docId,
                                            null
                                        )
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text(stringResource(Res.string.get_expiry_date))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isProcessing) {
                LoadingOverlay(message = stringResource(Res.string.extracting_expiry_date))
            }
        }
    }
}
