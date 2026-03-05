package com.stc.terminowo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

actual class FilePicker {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun pickFile(): PickedFile? = suspendCancellableCoroutine { cont ->
        val contentTypes = listOf(UTTypeImage, UTTypePDF)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes)
        picker.allowsMultipleSelection = false

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                if (url == null) {
                    cont.resume(null)
                    return
                }

                url.startAccessingSecurityScopedResource()
                try {
                    val data = NSData.dataWithContentsOfURL(url)
                    if (data == null) {
                        cont.resume(null)
                        return
                    }

                    val size = data.length.toInt()
                    val bytes = ByteArray(size)
                    bytes.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }

                    val fileName = url.lastPathComponent ?: "file"
                    val mimeType = when {
                        fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                        fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                        fileName.endsWith(".heic", ignoreCase = true) -> "image/heic"
                        else -> "image/jpeg"
                    }

                    cont.resume(PickedFile(bytes, mimeType, fileName))
                } finally {
                    url.stopAccessingSecurityScopedResource()
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                cont.resume(null)
            }
        }

        picker.delegate = delegate

        // Present the picker from the root view controller
        val rootVC = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(picker, animated = true, completion = null)
    }
}
