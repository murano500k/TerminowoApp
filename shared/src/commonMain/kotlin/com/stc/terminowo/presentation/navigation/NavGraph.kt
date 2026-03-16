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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp as vecDp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import com.stc.terminowo.presentation.camera.CameraScreen
import com.stc.terminowo.presentation.detail.DetailScreen
import com.stc.terminowo.presentation.main.DocumentStatusFilter
import com.stc.terminowo.presentation.main.DocumentsScreen
import com.stc.terminowo.presentation.preview.ImagePreviewScreen
import com.stc.terminowo.presentation.pulpit.DashboardScreen
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
import terminowo.shared.generated.resources.take_photo
import kotlin.uuid.Uuid

private val PulpitIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pulpit",
        defaultWidth = 24.vecDp,
        defaultHeight = 24.vecDp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF49454F))) {
            moveTo(5f, 21f)
            curveTo(4.45f, 21f, 3.975f, 20.8083f, 3.575f, 20.425f)
            curveTo(3.19167f, 20.025f, 3f, 19.55f, 3f, 19f)
            verticalLineTo(5f)
            curveTo(3f, 4.45f, 3.19167f, 3.98333f, 3.575f, 3.6f)
            curveTo(3.975f, 3.2f, 4.45f, 3f, 5f, 3f)
            horizontalLineTo(19f)
            curveTo(19.55f, 3f, 20.0167f, 3.2f, 20.4f, 3.6f)
            curveTo(20.8f, 3.98333f, 21f, 4.45f, 21f, 5f)
            verticalLineTo(19f)
            curveTo(21f, 19.55f, 20.8f, 20.025f, 20.4f, 20.425f)
            curveTo(20.0167f, 20.8083f, 19.55f, 21f, 19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 19f)
            horizontalLineTo(19f)
            verticalLineTo(16f)
            horizontalLineTo(16f)
            curveTo(15.5f, 16.6333f, 14.9f, 17.125f, 14.2f, 17.475f)
            curveTo(13.5167f, 17.825f, 12.7833f, 18f, 12f, 18f)
            curveTo(11.2167f, 18f, 10.475f, 17.825f, 9.775f, 17.475f)
            curveTo(9.09167f, 17.125f, 8.5f, 16.6333f, 8f, 16f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            close()
            moveTo(12f, 16f)
            curveTo(12.6333f, 16f, 13.2083f, 15.8167f, 13.725f, 15.45f)
            curveTo(14.2417f, 15.0833f, 14.6f, 14.6f, 14.8f, 14f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(14f)
            horizontalLineTo(9.2f)
            curveTo(9.4f, 14.6f, 9.75833f, 15.0833f, 10.275f, 15.45f)
            curveTo(10.7917f, 15.8167f, 11.3667f, 16f, 12f, 16f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val filePicker: FilePicker = koinInject()
    val imageStorage: ImageStorage = koinInject()
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = navBackStackEntry?.destination?.let { dest ->
        dest.hasRoute<Screen.Documents>() || dest.hasRoute<Screen.Pulpit>()
    } ?: true

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
                                popUpTo<Screen.Documents>()
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                val navBarColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFD32F2F),
                    selectedTextColor = Color(0xFFD32F2F),
                    indicatorColor = Color.Transparent
                )

                Box {
                    NavigationBar {
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
                            // Spacer item for center button
                            NavigationBarItem(
                                icon = {},
                                label = {},
                                selected = false,
                                enabled = false,
                                onClick = {}
                            )
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

                    if (isIos) {
                        FloatingActionButton(
                            onClick = { showAddSheet = true },
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 4.dp
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-20).dp)
                                .size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(Res.string.add_document)
                            )
                        }
                    }
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
                    onAddDocumentClick = { showAddSheet = true }
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
                    initialFilter = initialFilter
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
