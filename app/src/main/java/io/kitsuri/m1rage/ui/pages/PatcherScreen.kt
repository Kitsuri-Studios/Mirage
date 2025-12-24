package io.kitsuri.m1rage.ui.pages

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kitsuri.m1rage.R
import io.kitsuri.m1rage.ui.components.ShimmerAnimation
import io.kitsuri.m1rage.model.*
import io.kitsuri.m1rage.ui.components.InfoCard
import io.kitsuri.m1rage.ui.components.LocalSnackbarHost
import io.kitsuri.m1rage.ui.components.SelectionColumn
import io.kitsuri.m1rage.ui.components.SettingsCheckBox
import io.kitsuri.m1rage.ui.components.SupportCard
import io.kitsuri.m1rage.ui.components.TopBarConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    onTopBarConfigChanged: (TopBarConfig) -> Unit
) {
    val viewModel = viewModel<PatcherViewModel>()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel.patcherState) {
        onTopBarConfigChanged(
            if (viewModel.patcherState == PatcherState.CONFIGURATION) {
                TopBarConfig(
                    show = true,
                    content = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.patcher_title_configure)) },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        viewModel.dispatch(PatcherViewModel.ViewAction.Reset)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.patcher_close_desc)
                                    )
                                }
                            }
                        )
                    }
                )
            } else {
                TopBarConfig(show = true)
            }
        )
    }

    var showApkSourceDialog by remember { mutableStateOf(false) }
    var showSelectAppsScreen by remember { mutableStateOf(false) }
    var showActivitySelectorDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.dispatch(
                PatcherViewModel.ViewAction.StartDecompile(context, it, null)
            )
        }
    }

    if (showSelectAppsScreen) {
        SelectAppsScreen(
            onAppSelected = { packageName, appName, isSplit, splitApkPaths ->
                showSelectAppsScreen = false
                if (isSplit) {
                    viewModel.dispatch(
                        PatcherViewModel.ViewAction.StartDecompileFromSplits(
                            context, packageName, appName, splitApkPaths
                        )
                    )
                } else {
                    viewModel.dispatch(
                        PatcherViewModel.ViewAction.StartDecompile(context, null, splitApkPaths.first())
                    )
                }
            },
            onBackClick = { showSelectAppsScreen = false },
            onTopBarConfigChanged = onTopBarConfigChanged
        )
        return
    }

    CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                when (viewModel.patcherState) {
                    PatcherState.EMPTY -> {
                        FloatingActionButton(onClick = { showApkSourceDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.patcher_get_started_desc)
                            )
                        }
                    }
                    PatcherState.CONFIGURATION -> {
                        FloatingActionButton(
                            onClick = {
                                viewModel.dispatch(PatcherViewModel.ViewAction.StartPatch(context))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = stringResource(R.string.patcher_start_patch_desc)
                            )
                        }
                    }
                    else -> Unit
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                when (viewModel.patcherState) {
                    PatcherState.EMPTY -> EmptyPatcherView()
                    PatcherState.DECOMPILING -> DecompilingView(viewModel)
                    PatcherState.CONFIGURATION -> ConfigurationView(
                        viewModel = viewModel,
                        onActivitySelectorClick = { showActivitySelectorDialog = true }
                    )
                    PatcherState.PATCHING,
                    PatcherState.FINISHED,
                    PatcherState.ERROR -> PatchingView(viewModel)
                }
            }
        }
    }

    if (showApkSourceDialog) {
        ApkSourceDialog(
            onDismiss = { showApkSourceDialog = false },
            onStorageClick = {
                showApkSourceDialog = false
                apkPickerLauncher.launch("*/*")
            },
            onAppListClick = {
                showApkSourceDialog = false
                showSelectAppsScreen = true
            }
        )
    }

    if (showActivitySelectorDialog) {
        ActivitySelectorDialog(
            activities = viewModel.availableActivities,
            selectedActivity = viewModel.patchConfig.selectedActivity,
            onDismiss = { showActivitySelectorDialog = false },
            onActivitySelected = {
                viewModel.patchConfig = viewModel.patchConfig.copy(selectedActivity = it)
                showActivitySelectorDialog = false
            }
        )
    }
}

@Composable
private fun EmptyPatcherView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.patcher_about_mirage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.patcher_about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Box(modifier = Modifier.padding(vertical = 16.dp)) {
            InfoCard()
        }

        SupportCard()
    }
}

@Composable
private fun DecompilingView(viewModel: PatcherViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = { viewModel.decompileProgress },
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.patcher_decompiling))
            Text("${(viewModel.decompileProgress * 100).toInt()}%")
        }

        PatchLogsBody(
            logs = viewModel.logs,
            patchState = viewModel.patcherState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfigurationView(
    viewModel: PatcherViewModel,
    onActivitySelectorClick: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        viewModel.selectedApp?.let { app ->
            if (app.isSplitApk) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.patcher_split_apk_bundle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.patcher_split_count, app.splitCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.patcher_hxo_framework),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.patcher_hxo_injected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        SelectionColumn(Modifier.padding(horizontal = 24.dp)) {
            SelectionItem(
                selected = viewModel.patchConfig.mode == PatchMode.DEX,
                onClick = { viewModel.patchConfig = viewModel.patchConfig.copy(mode = PatchMode.DEX) },
                icon = Icons.Outlined.Construction,
                title = stringResource(R.string.patcher_mode_dex_title),
                desc = stringResource(R.string.patcher_mode_dex_desc),
                extraContent = null
            )

            SelectionItem(
                selected = viewModel.patchConfig.mode == PatchMode.MAPI,
                onClick = { viewModel.patchConfig = viewModel.patchConfig.copy(mode = PatchMode.MAPI) },
                icon = Icons.Outlined.Api,
                title = stringResource(R.string.patcher_mode_mapi_title),
                desc = stringResource(R.string.patcher_mode_mapi_desc),
                extraContent = null
            )
        }

        SettingsCheckBox(
            modifier = Modifier
                .padding(top = 6.dp)
                .clickable {
                    viewModel.patchConfig = viewModel.patchConfig.copy(debuggable = !viewModel.patchConfig.debuggable)
                },
            checked = viewModel.patchConfig.debuggable,
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.patcher_debuggable_title)
        )

        SettingsCheckBox(
            modifier = Modifier.clickable {
                viewModel.patchConfig = viewModel.patchConfig.copy(overrideVersionCode = !viewModel.patchConfig.overrideVersionCode)
            },
            checked = viewModel.patchConfig.overrideVersionCode,
            icon = Icons.Outlined.Numbers,
            title = stringResource(R.string.patcher_override_version_title),
            desc = stringResource(R.string.patcher_override_version_desc)
        )
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun PatchingView(viewModel: PatcherViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .animateContentSize()
    ) {
        PatchLogsBody(
            logs = viewModel.logs,
            patchState = viewModel.patcherState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        when (viewModel.patcherState) {
            PatcherState.FINISHED,
            PatcherState.ERROR -> {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val showSaveButton = viewModel.patcherState == PatcherState.FINISHED &&
                            viewModel.outputApkFile != null &&
                            !viewModel.savedToDownloads

                    AnimatedVisibility(
                        visible = showSaveButton,
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.dispatch(PatcherViewModel.ViewAction.SaveToDownloads(context))
                            }
                        ) {
                            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.patcher_save_button))
                        }
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.dispatch(PatcherViewModel.ViewAction.Reset)
                        }
                    ) {
                        Text(stringResource(R.string.patcher_return_button))
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun ApkSourceDialog(
    onDismiss: () -> Unit,
    onStorageClick: () -> Unit,
    onAppListClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.patcher_dialog_select_source)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStorageClick),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.patcher_source_storage))
                            Text(
                                text = stringResource(R.string.patcher_source_storage_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAppListClick),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Apps, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.patcher_source_apps))
                            Text(
                                text = stringResource(R.string.patcher_source_apps_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.patcher_dialog_cancel))
            }
        }
    )
}

@Composable
private fun ActivitySelectorDialog(
    activities: List<String>,
    selectedActivity: String?,
    onDismiss: () -> Unit,
    onActivitySelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.patcher_dialog_select_activity)) },
        text = {
            LazyColumn {
                items(activities) { activity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActivitySelected(activity) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activity == selectedActivity,
                            onClick = { onActivitySelected(activity) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activity.substringAfterLast('.'),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.patcher_dialog_close))
            }
        }
    )
}

@Composable
fun PatchLogsBody(
    logs: List<Pair<Int, String>>,
    patchState: PatcherState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val maxLogsHeight =
            if (patchState == PatcherState.PATCHING)
                maxHeight
            else
                maxHeight - ButtonDefaults.MinHeight - 12.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            ShimmerAnimation(enabled = patchState == PatcherState.PATCHING) { brush ->
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                ) {
                    val scrollState = rememberLazyListState()

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxLogsHeight)
                            .clip(RoundedCornerShape(24.dp))
                            .background(brush)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(logs) { (level, message) ->
                            Text(
                                text = message,
                                color = when (level) {
                                    Log.ERROR -> MaterialTheme.colorScheme.error
                                    Log.INFO -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            if (patchState == PatcherState.PATCHING) {
                                scrollState.scrollToItem(logs.lastIndex)
                            } else {
                                scrollState.animateScrollToItem(logs.lastIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}
