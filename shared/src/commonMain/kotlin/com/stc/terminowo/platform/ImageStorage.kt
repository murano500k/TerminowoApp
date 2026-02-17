package com.stc.terminowo.platform

expect class ImageStorage {
    suspend fun saveImage(imageBytes: ByteArray, fileName: String): String
    suspend fun saveThumbnail(imageBytes: ByteArray, fileName: String): String
    suspend fun deleteImage(path: String)
    suspend fun readImage(path: String): ByteArray?
}
