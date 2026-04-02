package com.stc.terminowo.presentation.navigation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.stc.terminowo.platform.FilePicker
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.isIos
import androidx.compose.runtime.collectAsState
import com.stc.terminowo.domain.repository.NotificationRepository
import com.stc.terminowo.presentation.camera.CameraScreen
import com.stc.terminowo.presentation.detail.DetailScreen
import com.stc.terminowo.presentation.main.DocumentStatusFilter
import com.stc.terminowo.presentation.main.DocumentsScreen
import com.stc.terminowo.presentation.notifications.NotificationsScreen
import com.stc.terminowo.presentation.preview.ImagePreviewScreen
import com.stc.terminowo.presentation.pulpit.DashboardScreen
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import com.stc.terminowo.presentation.theme.PulpitIcon
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock as DateTimeClock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.add_document
import terminowo.shared.generated.resources.choose_file
import terminowo.shared.generated.resources.enter_manually
import terminowo.shared.generated.resources.nav_documents
import terminowo.shared.generated.resources.document_added
import terminowo.shared.generated.resources.nav_pulpit
import terminowo.shared.generated.resources.pick_from_gallery
import terminowo.shared.generated.resources.take_photo
import kotlin.uuid.Uuid

@Composable
private fun AddDocumentOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val filePicker: FilePicker = koinInject()
    val imageStorage: ImageStorage = koinInject()
    val scope = rememberCoroutineScope()

    val notificationRepository: NotificationRepository = koinInject()
    val unreadNotificationCount by notificationRepository.getUnreadCount()
        .collectAsState(initial = 0L)

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = navBackStackEntry?.destination?.let { dest ->
        dest.hasRoute<Screen.Documents>() || dest.hasRoute<Screen.Pulpit>()
    } ?: true

    val accentRed = LocalExtendedColors.current.accentRed

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

                AddDocumentOption(
                    icon = Icons.Default.CameraAlt,
                    label = stringResource(Res.string.take_photo),
                    onClick = {
                        showAddSheet = false
                        navController.navigate(Screen.Camera)
                    }
                )

                if (isIos) {
                    AddDocumentOption(
                        icon = Icons.Default.Image,
                        label = stringResource(Res.string.pick_from_gallery),
                        onClick = {
                            showAddSheet = false
                            scope.launch {
                                val picked = filePicker.pickPhotoFromGallery() ?: return@launch
                                val timestamp = DateTimeClock.System.now().toEpochMilliseconds()
                                val fileName = "gallery_$timestamp.jpg"
                                val savedPath = imageStorage.saveImage(picked.bytes, fileName)
                                navController.navigate(
                                    Screen.ImagePreview(savedPath, picked.mimeType)
                                )
                            }
                        }
                    )
                }

                AddDocumentOption(
                    icon = Icons.Default.Description,
                    label = stringResource(Res.string.choose_file),
                    onClick = {
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
                )

                AddDocumentOption(
                    icon = Icons.Default.Edit,
                    label = stringResource(Res.string.enter_manually),
                    onClick = {
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
                            popUpTo<Screen.Documents>()
                        }
                    }
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showBottomBar && !isIos) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = accentRed,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_document)
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                val navBarColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accentRed,
                    selectedTextColor = accentRed,
                    indicatorColor = Color.Transparent
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val currentDest = navBackStackEntry?.destination

                    val pulpitSelected = currentDest?.hasRoute<Screen.Pulpit>() == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = PulpitIcon,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(Res.string.nav_pulpit)) },
                        selected = pulpitSelected,
                        colors = navBarColors,
                        onClick = {
                            navController.navigate(Screen.Pulpit) {
                                popUpTo<Screen.Documents>() { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    if (isIos) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(52.dp)
                                    .background(
                                        color = accentRed,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { showAddSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(Res.string.add_document),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    val documentsSelected = currentDest?.hasRoute<Screen.Documents>() == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (documentsSelected) Icons.Default.Folder else Icons.Outlined.Folder,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(Res.string.nav_documents)) },
                        selected = documentsSelected,
                        colors = navBarColors,
                        onClick = {
                            navController.navigate(Screen.Documents()) {
                                popUpTo<Screen.Documents>() { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Documents(),
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            composable<Screen.Pulpit> {
                DashboardScreen(
                    onDocumentClick = { id ->
                        navController.navigate(Screen.DetailEdit(id))
                    },
                    onNavigateToDocuments = {
                        navController.navigate(Screen.Documents()) {
                            popUpTo<Screen.Documents> { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToDocumentsWithFilter = { filter ->
                        navController.navigate(Screen.Documents(initialFilter = filter.name)) {
                            popUpTo<Screen.Documents> { inclusive = true }
                        }
                    },
                    onAddDocumentClick = { showAddSheet = true },
                    onNotificationsClick = { navController.navigate(Screen.Notifications) },
                    unreadNotificationCount = unreadNotificationCount.toInt()
                )
            }

            composable<Screen.Documents> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.Documents>()
                val initialFilter = route.initialFilter?.let {
                    try { DocumentStatusFilter.valueOf(it) } catch (_: Exception) { null }
                }
                DocumentsScreen(
                    onScanClick = { showAddSheet = true },
                    onDocumentClick = { documentId ->
                        navController.navigate(Screen.DetailEdit(documentId))
                    },
                    initialFilter = initialFilter,
                    onNotificationsClick = { navController.navigate(Screen.Notifications) },
                    unreadNotificationCount = unreadNotificationCount.toInt()
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
                            popUpTo<Screen.Documents>()
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
                    onSaved = { docName ->
                        navController.navigate(Screen.Documents()) {
                            popUpTo<Screen.Documents> { inclusive = true }
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(getString(Res.string.document_added, docName))
                        }
                    },
                    onDeleted = {
                        navController.navigate(Screen.Documents()) {
                            popUpTo<Screen.Documents> { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Notifications> {
                NotificationsScreen(
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
                    onSaved = { _ -> navController.popBackStack() },
                    onDeleted = {
                        navController.navigate(Screen.Documents()) {
                            popUpTo<Screen.Documents> { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
