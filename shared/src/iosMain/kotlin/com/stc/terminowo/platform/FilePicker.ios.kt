package com.stc.terminowo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

actual class FilePicker {

    // UIKit holds picker delegates weakly — keep a strong reference while presented
    private var delegateHolder: NSObject? = null

    actual suspend fun pickPhotoFromGallery(): PickedFile? = suspendCancellableCoroutine { cont ->
        val presenter = topViewController()
        if (presenter == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = configuration)

        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, completion = null)
                delegateHolder = null

                val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
                if (provider == null) {
                    cont.resume(null)
                    return
                }
                // Loads the original asset data (HEIC, PNG, JPEG...) on a background queue
                provider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, error ->
                    if (error != null) {
                        AppLogger.e("FilePicker", "Failed to load gallery image: ${error.localizedDescription}", null)
                    }
                    val jpeg = data?.let { toJpeg(it) }
                    cont.resume(jpeg?.let { PickedFile(it, "image/jpeg", "gallery_photo.jpg") })
                }
            }
        }

        delegateHolder = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)

        cont.invokeOnCancellation {
            picker.dismissViewControllerAnimated(false, completion = null)
        }
    }

    actual suspend fun pickFile(): PickedFile? = suspendCancellableCoroutine { cont ->
        val presenter = topViewController()
        if (presenter == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeImage, UTTypePDF))
        picker.allowsMultipleSelection = false

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                delegateHolder = null
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                cont.resume(url?.let { readPickedFile(it) })
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                delegateHolder = null
                cont.resume(null)
            }
        }

        delegateHolder = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)

        cont.invokeOnCancellation {
            picker.dismissViewControllerAnimated(false, completion = null)
        }
    }

    private fun readPickedFile(url: NSURL): PickedFile? {
        val accessing = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url) ?: return null
            val fileName = url.lastPathComponent ?: "file"
            return if (fileName.endsWith(".pdf", ignoreCase = true)) {
                data.toByteArray()?.let { PickedFile(it, "application/pdf", fileName) }
            } else {
                // OCR backend doesn't accept HEIC etc. — normalize every image to JPEG
                toJpeg(data)?.let { PickedFile(it, "image/jpeg", fileName) }
            }
        } finally {
            if (accessing) url.stopAccessingSecurityScopedResource()
        }
    }

    private fun toJpeg(data: NSData): ByteArray? {
        val image = UIImage(data = data) ?: return null
        return UIImageJPEGRepresentation(image, 0.95)?.toByteArray()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray? {
        val size = length.toInt()
        if (size == 0) return null
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length)
        }
        return bytes
    }
}
