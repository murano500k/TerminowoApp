package com.stc.terminowo.platform

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FilePicker(private val context: Context) {

    companion object {
        var pickerLauncher: ((onResult: (Uri?) -> Unit) -> Unit)? = null
    }

    actual suspend fun pickPhotoFromGallery(): PickedFile? = null

    actual suspend fun pickFile(): PickedFile? = suspendCancellableCoroutine { cont ->
        val launcher = pickerLauncher
        if (launcher == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        launcher { uri ->
            if (uri == null) {
                cont.resume(null)
                return@launcher
            }

            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    cont.resume(null)
                    return@launcher
                }

                val fileName = uri.lastPathSegment ?: "file"
                cont.resume(PickedFile(bytes, mimeType, fileName))
            } catch (e: Exception) {
                cont.resume(null)
            }
        }
    }
}
