@file:OptIn(ExperimentalForeignApi::class)

package com.stc.terminowo.presentation.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stc.terminowo.platform.ImageStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock as DateTimeClock
import org.koin.compose.koinInject
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.Foundation.setValue
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
    return bytes
}

@Composable
actual fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onBack: () -> Unit
) {
    val imageStorage: ImageStorage = koinInject()
    val delegateHolder = remember { DelegateHolder() }

    LaunchedEffect(Unit) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootVC == null) {
            onBack()
            return@LaunchedEffect
        }

        val picker = UIImagePickerController()
        // Set sourceType to Camera (=1) via KVC to bypass deprecated enum type
        picker.setValue(NSNumber(int = 1), forKey = "sourceType")

        val imageBytes = suspendCancellableCoroutine { continuation ->
            val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol,
                UINavigationControllerDelegateProtocol {

                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>
                ) {
                    val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                    val bytes = image?.let { img ->
                        UIImageJPEGRepresentation(img, 0.95)?.toByteArray()
                    }
                    picker.dismissViewControllerAnimated(true, completion = null)
                    continuation.resume(bytes)
                }

                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                    picker.dismissViewControllerAnimated(true, completion = null)
                    continuation.resume(null)
                }
            }

            delegateHolder.delegate = delegate
            picker.delegate = delegate
            rootVC.presentViewController(picker, animated = true, completion = null)

            continuation.invokeOnCancellation {
                picker.dismissViewControllerAnimated(false, completion = null)
            }
        }

        if (imageBytes != null) {
            val fileName = "doc_${DateTimeClock.System.now().toEpochMilliseconds()}.jpg"
            val savedPath = imageStorage.saveImage(imageBytes, fileName)
            onImageCaptured(savedPath)
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private class DelegateHolder {
    var delegate: NSObject? = null
}
