package io.kitsuri.m1rage.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.kitsuri.m1rage.model.PatchedAppInfo
import io.kitsuri.m1rage.ui.components.ModItem
import io.kitsuri.m1rage.ui.dialogs.ComposedDialog
import io.kitsuri.m1rage.ui.dialogs.ComposedChoiceDialog
import io.kitsuri.m1rage.utils.AppLibraryUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModsScreen(
    app: PatchedAppInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var allMods by remember { mutableStateOf(AppLibraryUtils.getAllMods(context, app.packageName) ?: emptyMap()) }
    var modOrder by remember { mutableStateOf(allMods.keys.toList()) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedForDeletion by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf("") }
    var showImportMessage by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(allMods) {
        modOrder = allMods.keys.toList()
    }

    val singleModLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "mod.so"

                if (!fileName.endsWith(".so") && !fileName.endsWith(".hxo")) {
                    importMessage = "Only .so or .hxo files are supported"
                    showImportMessage = true
                    return@rememberLauncherForActivityResult
                }

                inputStream?.use { input ->
                    val cacheFile = File(context.cacheDir, fileName)
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }

                    if (AppLibraryUtils.copyModFile(context, app.packageName, cacheFile, fileName) != null) {
                        AppLibraryUtils.updateModulesJson(context, app.packageName, fileName)
                        cacheFile.delete()
                        importMessage = "Mod imported successfully"
                        showImportMessage = true
                        allMods = AppLibraryUtils.getAllMods(context, app.packageName) ?: emptyMap()
                        modOrder = allMods.keys.toList()
                    } else {
                        cacheFile.delete()
                        importMessage = "Failed to import mod"
                        showImportMessage = true
                    }
                }
            } catch (e: Exception) {
                Log.e("ModImport", "Error importing mod", e)
                importMessage = "Error: ${e.message}"
                showImportMessage = true
            }
        }
        showImportDialog = false
    }

    val zipModLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val zipFile = File(context.cacheDir, "mods.zip")

                inputStream?.use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                var importedCount = 0
                val zipInputStream = java.util.zip.ZipInputStream(zipFile.inputStream())
                var entry = zipInputStream.nextEntry

                while (entry != null) {
                    if (!entry.isDirectory && (entry.name.endsWith(".so") || entry.name.endsWith(".hxo"))) {
                        val fileName = File(entry.name).name
                        val cacheFile = File(context.cacheDir, fileName)

                        cacheFile.outputStream().use { output ->
                            zipInputStream.copyTo(output)
                        }

                        if (AppLibraryUtils.copyModFile(context, app.packageName, cacheFile, fileName) != null) {
                            AppLibraryUtils.updateModulesJson(context, app.packageName, fileName)
                            cacheFile.delete()
                            importedCount++
                        } else {
                            cacheFile.delete()
                        }
                    }
                    entry = zipInputStream.nextEntry
                }

                zipInputStream.close()
                zipFile.delete()

                if (importedCount > 0) {
                    importMessage = "Imported $importedCount mod(s)"
                    allMods = AppLibraryUtils.getAllMods(context, app.packageName) ?: emptyMap()
                    modOrder = allMods.keys.toList()
                } else {
                    importMessage = "Failed to import mods from ZIP"
                }
                showImportMessage = true
            } catch (e: Exception) {
                Log.e("ModImport", "Error importing mods zip", e)
                importMessage = "Error: ${e.message}"
                showImportMessage = true
            }
        }
        showImportDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${app.appName} - Mods") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (allMods.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No Mods Imported",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to import mods",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(modOrder, key = { _, item -> item }) { index, modName ->
                        val isEnabled = allMods[modName] ?: false
                        ModItem(
                            modName = modName,
                            loadOrder = index + 1,
                            isEnabled = isEnabled,
                            isDeleteMode = isDeleteMode,
                            isSelected = selectedForDeletion.contains(modName),
                            isDragging = draggedIndex == index,
                            onToggleEnabled = {
                                AppLibraryUtils.toggleMod(context, app.packageName, modName, !isEnabled)
                                allMods = AppLibraryUtils.getAllMods(context, app.packageName) ?: emptyMap()
                            },
                            onToggleSelection = {
                                selectedForDeletion = if (selectedForDeletion.contains(modName)) {
                                    selectedForDeletion - modName
                                } else {
                                    selectedForDeletion + modName
                                }
                            },
                            onDragStart = { draggedIndex = index },
                            onDragEnd = { draggedIndex = null },
                            onMove = { fromIndex, toIndex ->
                                val newOrder = modOrder.toMutableList()
                                val item = newOrder.removeAt(fromIndex)
                                newOrder.add(toIndex, item)
                                modOrder = newOrder
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // FAB Menu
            ModFABMenu(
                expanded = fabMenuExpanded,
                onExpandChange = { fabMenuExpanded = it },
                isDeleteMode = isDeleteMode,
                selectedCount = selectedForDeletion.size,
                onImportSingle = { singleModLauncher.launch("*/*") },
                onImportZip = { zipModLauncher.launch("application/zip") },
                onDeleteMode = {
                    isDeleteMode = true
                    selectedForDeletion = emptySet()
                    fabMenuExpanded = false
                },
                onCancelDelete = {
                    isDeleteMode = false
                    selectedForDeletion = emptySet()
                },
                onConfirmDelete = {
                    if (selectedForDeletion.isNotEmpty()) {
                        showDeleteConfirmation = true
                    }
                }
            )
        }
    }

    if (showDeleteConfirmation) {
        ComposedDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = "Delete Mods",
            message = "Are you sure you want to delete ${selectedForDeletion.size} mod(s)?",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                selectedForDeletion.forEach { modName ->
                    AppLibraryUtils.removeMod(context, app.packageName, modName)
                }
                selectedForDeletion = emptySet()
                isDeleteMode = false
                showDeleteConfirmation = false
                allMods = AppLibraryUtils.getAllMods(context, app.packageName) ?: emptyMap()
                modOrder = allMods.keys.toList()
            }
        )
    }

    if (showImportDialog) {
        ComposedChoiceDialog(
            onDismissRequest = { showImportDialog = false },
            title = "Import Mods",
            message = "Choose import method",
            option1 = "Single Mod" to { singleModLauncher.launch("*/*") },
            option2 = "Import ZIP" to { zipModLauncher.launch("application/zip") }
        )
    }

    if (showImportMessage) {
        ComposedDialog(
            onDismissRequest = { showImportMessage = false },
            title = if (importMessage.contains("Error") || importMessage.contains("Failed") || importMessage.contains("Only")) "Error" else "Success",
            message = importMessage,
            confirmText = "OK",
            isDestructive = false,
            onConfirm = { showImportMessage = false }
        )
    }
}

@Composable
private fun ModFABMenu(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    isDeleteMode: Boolean,
    selectedCount: Int,
    onImportSingle: () -> Unit,
    onImportZip: () -> Unit,
    onDeleteMode: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isDeleteMode) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MiniFAB(
                            icon = Icons.Default.Close,
                            label = "Cancel",
                            onClick = onCancelDelete
                        )
                        MiniFAB(
                            icon = Icons.Default.Delete,
                            label = if (selectedCount > 0) "Delete ($selectedCount)" else "Select items",
                            onClick = onConfirmDelete,
                            containerColor = if (selectedCount > 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = expanded,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() + scaleIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MiniFAB(
                            icon = Icons.Default.Upload,
                            label = "Import Single",
                            onClick = {
                                onImportSingle()
                                onExpandChange(false)
                            }
                        )
                        MiniFAB(
                            icon = Icons.Default.FolderZip,
                            label = "Import ZIP",
                            onClick = {
                                onImportZip()
                                onExpandChange(false)
                            }
                        )
                        MiniFAB(
                            icon = Icons.Default.Delete,
                            label = "Delete Mode",
                            onClick = onDeleteMode,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { onExpandChange(!expanded) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Menu",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniFAB(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}