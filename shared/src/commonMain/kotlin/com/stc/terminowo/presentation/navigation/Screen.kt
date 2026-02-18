package com.stc.terminowo.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Main : Screen

    @Serializable
    data object Camera : Screen

    @Serializable
    data class ImagePreview(val imagePath: String) : Screen

    @Serializable
    data class DetailNew(
        val name: String?,
        val expiryDate: String?,
        val confidence: Float?,
        val imagePath: String,
        val thumbnailPath: String,
        val rawOcrResponse: String?,
        val documentId: String,
        val category: String? = null
    ) : Screen

    @Serializable
    data class DetailEdit(val documentId: String) : Screen
}
