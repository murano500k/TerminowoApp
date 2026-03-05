package com.stc.terminowo.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.stc.terminowo.platform.FilePicker
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.presentation.camera.CameraScreen
import com.stc.terminowo.presentation.categories.CategoryListScreen
import com.stc.terminowo.presentation.detail.DetailScreen
import com.stc.terminowo.presentation.main.DocumentListScreen
import com.stc.terminowo.presentation.preview.ImagePreviewScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import terminowo.shared.generated.resources.Res
import kotlin.uuid.Uuid
import terminowo.shared.generated.resources.add_document
import terminowo.shared.generated.resources.choose_file
import terminowo.shared.generated.resources.enter_manually
import terminowo.shared.generated.resources.take_photo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val filePicker: FilePicker = koinInject()
    val imageStorage: ImageStorage = koinInject()
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(Res.string.add_document),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddSheet = false
                            navController.navigate(Screen.Camera)
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(Res.string.take_photo),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddSheet = false
                            scope.launch {
                                val picked = filePicker.pickFile() ?: return@launch
                                val isPdf = picked.mimeType == "application/pdf"
                                val ext = if (isPdf) "pdf" else "jpg"
                                val timestamp = DateTimeClock.System.now().toEpochMilliseconds()
                                val fileName = "picked_$timestamp.$ext"

                                val savedPath = if (isPdf) {
                                    imageStorage.saveRawFile(picked.bytes, fileName)
                                } else {
                                    imageStorage.saveImage(picked.bytes, fileName)
                                }

                                navController.navigate(
                                    Screen.ImagePreview(savedPath, picked.mimeType)
                                )
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(Res.string.choose_file),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddSheet = false
                            val uuid = Uuid.random().toString()
                            navController.navigate(
                                Screen.DetailNew(
                                    name = null,
                                    expiryDate = null,
                                    confidence = null,
                                    imagePath = "",
                                    thumbnailPath = "",
                                    rawOcrResponse = null,
                                    documentId = uuid,
                                    category = null
                                )
                            ) {
                                popUpTo<Screen.Categories>()
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(Res.string.enter_manually),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Categories
    ) {
        composable<Screen.Categories> {
            CategoryListScreen(
                onScanClick = { showAddSheet = true },
                onCategoryClick = { categoryKey ->
                    navController.navigate(Screen.DocumentList(categoryKey))
                },
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.DetailEdit(documentId))
                }
            )
        }

        composable<Screen.DocumentList> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.DocumentList>()
            DocumentListScreen(
                categoryKey = route.categoryKey,
                onScanClick = { showAddSheet = true },
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
                mimeType = route.mimeType,
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
