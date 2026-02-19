package com.stc.terminowo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.stc.terminowo.presentation.camera.CameraScreen
import com.stc.terminowo.presentation.categories.CategoryListScreen
import com.stc.terminowo.presentation.detail.DetailScreen
import com.stc.terminowo.presentation.main.DocumentListScreen
import com.stc.terminowo.presentation.preview.ImagePreviewScreen

@Composable
fun NavGraph(isAuthenticated: Boolean = false) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Categories
    ) {
        composable<Screen.Categories> {
            CategoryListScreen(
                onScanClick = { navController.navigate(Screen.Camera) },
                onCategoryClick = { categoryKey ->
                    navController.navigate(Screen.DocumentList(categoryKey))
                }
            )
        }

        composable<Screen.DocumentList> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.DocumentList>()
            DocumentListScreen(
                categoryKey = route.categoryKey,
                onScanClick = { navController.navigate(Screen.Camera) },
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.DetailEdit(documentId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Camera> {
            CameraScreen(
                onImageCaptured = { imagePath ->
                    navController.navigate(Screen.ImagePreview(imagePath)) {
                        popUpTo<Screen.Camera> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ImagePreview> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.ImagePreview>()
            ImagePreviewScreen(
                imagePath = route.imagePath,
                isAuthenticated = isAuthenticated,
                onRetake = {
                    navController.navigate(Screen.Camera) {
                        popUpTo<Screen.ImagePreview> { inclusive = true }
                    }
                },
                onScanResult = { name, expiryDate, confidence, imgPath, thumbPath, rawResponse, docId, category ->
                    navController.navigate(
                        Screen.DetailNew(
                            name = name,
                            expiryDate = expiryDate,
                            confidence = confidence,
                            imagePath = imgPath,
                            thumbnailPath = thumbPath,
                            rawOcrResponse = rawResponse,
                            documentId = docId,
                            category = category
                        )
                    ) {
                        popUpTo<Screen.Categories>()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.DetailNew> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.DetailNew>()
            DetailScreen(
                isNew = true,
                documentId = null,
                newDocName = route.name,
                newDocExpiryDate = route.expiryDate,
                newDocConfidence = route.confidence,
                newDocImagePath = route.imagePath,
                newDocThumbnailPath = route.thumbnailPath,
                newDocRawResponse = route.rawOcrResponse,
                newDocId = route.documentId,
                newDocCategory = route.category,
                onSaved = {
                    navController.navigate(Screen.Categories) {
                        popUpTo<Screen.Categories> { inclusive = true }
                    }
                },
                onDeleted = {
                    navController.navigate(Screen.Categories) {
                        popUpTo<Screen.Categories> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.DetailEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.DetailEdit>()
            DetailScreen(
                isNew = false,
                documentId = route.documentId,
                newDocName = null,
                newDocExpiryDate = null,
                newDocConfidence = null,
                newDocImagePath = null,
                newDocThumbnailPath = null,
                newDocRawResponse = null,
                newDocId = null,
                newDocCategory = null,
                onSaved = { navController.popBackStack() },
                onDeleted = {
                    navController.navigate(Screen.Categories) {
                        popUpTo<Screen.Categories> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
