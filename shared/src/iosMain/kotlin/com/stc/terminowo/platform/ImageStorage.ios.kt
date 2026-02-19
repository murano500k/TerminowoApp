package com.stc.terminowo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientationUp
import platform.posix.memcpy

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

    private val imagesDir: String by lazy {
        val dir = "${getDocumentsDir()}/documents"
        fileManager.createDirectoryAtPath(
            dir, withIntermediateDirectories = true, attributes = null, error = null
        )
        dir
    }

    private val thumbnailsDir: String by lazy {
        val dir = "${getDocumentsDir()}/thumbnails"
        fileManager.createDirectoryAtPath(
            dir, withIntermediateDirectories = true, attributes = null, error = null
        )
        dir
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return ByteArray(0)
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
        return bytes
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.Default) {
            val data = normalizeImageRotation(imageBytes)
            val path = "$imagesDir/$fileName"
            data.writeToFile(path, atomically = true)
            path
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun normalizeImageRotation(imageBytes: ByteArray): NSData {
        val nsData = imageBytes.toNSData()
        val image = UIImage(data = nsData) ?: return nsData

        if (image.imageOrientation == UIImageOrientationUp) {
            return nsData
        }

        // Draw into a new context to bake in the correct EXIF rotation
        val imageWidth = image.size.useContents { width }
        val imageHeight = image.size.useContents { height }

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(imageWidth, imageHeight), false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, imageWidth, imageHeight))
        val normalizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return normalizedImage?.let { UIImageJPEGRepresentation(it, 0.95) } ?: nsData
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveThumbnail(imageBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.Default) {
            val nsData = imageBytes.toNSData()
            val image = UIImage(data = nsData)
                ?: return@withContext "$thumbnailsDir/thumb_$fileName"

            val originalWidth = image.size.useContents { width }
            val originalHeight = image.size.useContents { height }

            val targetWidth = 200.0
            val targetHeight = targetWidth * originalHeight / originalWidth

            UIGraphicsBeginImageContextWithOptions(
                CGSizeMake(targetWidth, targetHeight), false, 1.0
            )
            image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
            val thumbnailImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            val path = "$thumbnailsDir/thumb_$fileName"
            thumbnailImage?.let {
                UIImageJPEGRepresentation(it, 0.80)?.writeToFile(path, atomically = true)
            }
            path
        }

    actual suspend fun deleteImage(path: String) {
        withContext(Dispatchers.Default) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readImage(path: String): ByteArray? =
        withContext(Dispatchers.Default) {
            if (!fileManager.fileExistsAtPath(path)) return@withContext null
            NSData.dataWithContentsOfFile(path)?.toByteArray()
        }
}
