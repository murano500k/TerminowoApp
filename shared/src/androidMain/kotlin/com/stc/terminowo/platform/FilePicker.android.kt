package com.stc.terminowo.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

actual class FilePicker(private val context: Context) {

    companion object {
        var pickerLauncher: ((onResult: (Uri?) -> Unit) -> Unit)? = null
        var galleryLauncher: ((onResult: (Uri?) -> Unit) -> Unit)? = null
    }

    actual suspend fun pickPhotoFromGallery(): PickedFile? {
        val uri = awaitUri(galleryLauncher) ?: return null
        return readPickedFile(uri)
    }

    actual suspend fun pickFile(): PickedFile? {
        val uri = awaitUri(pickerLauncher) ?: return null
        return readPickedFile(uri)
    }

    private suspend fun awaitUri(
        launcher: ((onResult: (Uri?) -> Unit) -> Unit)?
    ): Uri? = suspendCancellableCoroutine { cont ->
        if (launcher == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        launcher { uri -> if (cont.isActive) cont.resume(uri) }
    }

    private suspend fun readPickedFile(uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val fileName = uri.lastPathSegment ?: "file"

            if (mimeType == "application/pdf") {
                PickedFile(bytes, mimeType, fileName)
            } else {
                // OCR backend doesn't accept HEIC etc. — normalize every image to JPEG
                val jpeg = toJpeg(bytes, mimeType) ?: return@withContext null
                PickedFile(jpeg, "image/jpeg", fileName)
            }
        } catch (e: Exception) {
            AppLogger.e("FilePicker", "Failed to read picked file", e)
            null
        }
    }

    private fun toJpeg(bytes: ByteArray, mimeType: String): ByteArray? {
        if (mimeType == "image/jpeg" || mimeType == "image/jpg") return bytes

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder supports HEIC/AVIF and applies EXIF orientation
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } ?: return null

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        bitmap.recycle()
        return output.toByteArray()
    }
}
