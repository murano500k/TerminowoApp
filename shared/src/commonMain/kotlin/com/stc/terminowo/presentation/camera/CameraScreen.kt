package com.stc.terminowo.presentation.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import com.stc.terminowo.platform.ImageStorage
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.back
import terminowo.shared.generated.resources.camera_permission_required
import terminowo.shared.generated.resources.capture_failed
import terminowo.shared.generated.resources.failed_read_captured_image
import terminowo.shared.generated.resources.go_back
import terminowo.shared.generated.resources.scan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val imageStorage: ImageStorage = koinInject()

    val permissions = providePermissions()
    var permissionGranted by remember { mutableStateOf(permissions.hasCameraPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    if (!permissionGranted) {
        if (permissionDenied) {
            // Permission was denied — show fallback
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.camera_permission_required),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text(stringResource(Res.string.go_back))
                    }
                }
            }
            return
        }

        permissions.RequestCameraPermission(
            onGranted = { permissionGranted = true },
            onDenied = { permissionDenied = true }
        )
        return
    }

    // Camera is permitted — show live preview
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraState by rememberCameraKState(
        config = CameraConfiguration(
            cameraLens = CameraLens.BACK,
            flashMode = FlashMode.OFF,
            imageFormat = ImageFormat.JPEG
        )
    )

    CameraKScreen(
        cameraState = cameraState,
        modifier = Modifier.fillMaxSize()
    ) { readyState ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Capture button
            Button(
                onClick = {
                    if (isCapturing) return@Button
                    isCapturing = true
                    errorMessage = null
                    scope.launch {
                        val failedReadMsg = getString(Res.string.failed_read_captured_image)
                        val captureFailedMsg = getString(Res.string.capture_failed)
                        try {
                            when (val result = readyState.controller.takePictureToFile()) {
                                is ImageCaptureResult.SuccessWithFile -> {
                                    // Read file bytes and re-save to our app's documents dir
                                    val fileBytes = imageStorage.readImage(result.filePath)
                                    if (fileBytes != null) {
                                        val fileName = "doc_${DateTimeClock.System.now().toEpochMilliseconds()}.jpg"
                                        val savedPath = imageStorage.saveImage(fileBytes, fileName)
                                        onImageCaptured(savedPath)
                                    } else {
                                        errorMessage = failedReadMsg
                                        isCapturing = false
                                    }
                                }
                                is ImageCaptureResult.Success -> {
                                    val fileName = "doc_${DateTimeClock.System.now().toEpochMilliseconds()}.jpg"
                                    val savedPath = imageStorage.saveImage(result.byteArray, fileName)
                                    onImageCaptured(savedPath)
                                }
                                is ImageCaptureResult.Error -> {
                                    errorMessage = result.exception.message ?: captureFailedMsg
                                    isCapturing = false
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: captureFailedMsg
                            isCapturing = false
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isCapturing,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.scan),
                        maxLines = 1
                    )
                }
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 130.dp)
                )
            }

            // Back button overlay
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}
