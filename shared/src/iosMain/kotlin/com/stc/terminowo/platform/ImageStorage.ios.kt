package com.stc.terminowo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class ImageStorage {
    private val fileManager = NSFileManager.defaultManager

    @OptIn(ExperimentalForeignApi::class)
    private fun getDocumentsDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        return paths.first() as String
    }

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String): String {
        // TODO: Implement iOS image saving
        return "${getDocumentsDir()}/documents/$fileName"
    }

    actual suspend fun saveThumbnail(imageBytes: ByteArray, fileName: String): String {
        // TODO: Implement iOS thumbnail generation
        return "${getDocumentsDir()}/thumbnails/thumb_$fileName"
    }

    actual suspend fun deleteImage(path: String) {
        // TODO: Implement iOS image deletion
    }

    actual suspend fun readImage(path: String): ByteArray? {
        // TODO: Implement iOS image reading
        return null
    }
}
