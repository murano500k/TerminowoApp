package com.stc.terminowo.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual class ImageStorage(
    private val context: Context
) {
    private val imagesDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val file = File(imagesDir, fileName)
            file.writeBytes(imageBytes)
            file.absolutePath
        }

    actual suspend fun saveThumbnail(imageBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val thumbnailWidth = 200
            val thumbnailHeight = (thumbnailWidth / aspectRatio).toInt()
            val thumbnail = Bitmap.createScaledBitmap(bitmap, thumbnailWidth, thumbnailHeight, true)

            val file = File(thumbnailsDir, "thumb_$fileName")
            FileOutputStream(file).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()
            thumbnail.recycle()
            file.absolutePath
        }

    actual suspend fun deleteImage(path: String) {
        withContext(Dispatchers.IO) {
            File(path).delete()
        }
    }

    actual suspend fun readImage(path: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (file.exists()) file.readBytes() else null
        }
}
