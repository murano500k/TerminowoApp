package com.stc.terminowo.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
            val normalized = normalizeImageRotation(imageBytes)
            val file = File(imagesDir, fileName)
            file.writeBytes(normalized)
            file.absolutePath
        }

    private fun normalizeImageRotation(imageBytes: ByteArray): ByteArray {
        val exif = ExifInterface(ByteArrayInputStream(imageBytes))
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
        ) {
            return imageBytes
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return imageBytes
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        val output = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 95, output)
        if (rotated !== original) rotated.recycle()
        original.recycle()
        return output.toByteArray()
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
