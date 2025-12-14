package io.kitsuri.m1rage.ui.pages

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kitsuri.m1rage.ui.components.ShimmerAnimation
import io.kitsuri.m1rage.model.*
import io.kitsuri.m1rage.ui.components.SelectionColumn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    onConfigureStateChanged: (Boolean) -> Unit
)
 {
    val viewModel = viewModel<PatcherViewModel>()

     LaunchedEffect(viewModel.patcherState) {
         onConfigureStateChanged(
             viewModel.patcherState == PatcherState.CONFIGURATION
         )
     }


    var showApkSourceDialog by remember { mutableStateOf(false) }
    var showInstalledAppsDialog by remember { mutableStateOf(false) }
    var showActivitySelectorDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.dispatch(
                PatcherViewModel.ViewAction.StartDecompile(context, it, null)
            )
        }
    }

     Scaffold(
         topBar = {
             if (viewModel.patcherState == PatcherState.CONFIGURATION) {
                 TopAppBar(
                     title = { Text("Configure Patch") },
                     navigationIcon = {
                         IconButton(
                             onClick = {
                                 viewModel.dispatch(PatcherViewModel.ViewAction.Reset)
                             }
                         ) {
                             Icon(
                                 imageVector = Icons.Outlined.ArrowBack,
                                 contentDescription = "Back"
                             )
                         }
                     }
                 )
             }
         },
         floatingActionButton = {
             when (viewModel.patcherState) {
                 PatcherState.EMPTY -> {
                     FloatingActionButton(onClick = { showApkSourceDialog = true }) {
                         Icon(Icons.Filled.PlayArrow, null)
                     }
                 }
                 PatcherState.CONFIGURATION -> {
                     ExtendedFloatingActionButton(
                         onClick = {
                             viewModel.dispatch(
                                 PatcherViewModel.ViewAction.StartPatch(context)
                             )
                         },
                         icon = { Icon(Icons.Outlined.AutoFixHigh, null) },
                         text = { Text("Start Patch") }
                     )
                 }
                 else -> Unit
             }
         }
     )
 { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                PatcherState.ERROR -> {
                    PatchingView(viewModel)
                }
            }
        }
    }

    if (showApkSourceDialog) {
        ApkSourceDialog(
            onDismiss = { showApkSourceDialog = false },
            onStorageClick = {
                showApkSourceDialog = false
                apkPickerLauncher.launch("application/vnd.android.package-archive")
            },
            onAppListClick = {
                showApkSourceDialog = false
                showInstalledAppsDialog = true
            }
        )
    }

    if (showInstalledAppsDialog) {
        InstalledAppsDialog(
            context = context,
            onDismiss = { showInstalledAppsDialog = false },
            onAppSelected = { _, _, apkPath ->
                showInstalledAppsDialog = false
                viewModel.dispatch(
                    PatcherViewModel.ViewAction.StartDecompile(context, null, apkPath)
                )
            }
        )
    }

    if (showActivitySelectorDialog) {
        ActivitySelectorDialog(
            activities = viewModel.availableActivities,
            selectedActivity = viewModel.patchConfig.selectedActivity,
            onDismiss = { showActivitySelectorDialog = false },
            onActivitySelected = {
                viewModel.patchConfig =
                    viewModel.patchConfig.copy(selectedActivity = it)
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Text(
            text = "Mirage Patcher",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About Mirage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "An Android Application to Repack APKs with HXO Framework for Dynamic Mod Loading",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

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
            Text("Decompiling APK…")
            Text("${(viewModel.decompileProgress * 100).toInt()}%")
        }

        PatchLogsBody(
            logs = viewModel.logs,
            patchState = viewModel.patcherState,
            modifier = Modifier
                .fillMaxWidth()
             //   .padding(horizontal = 16.dp)
        )

    }
}


@Composable
private fun ConfigurationView(
    viewModel: PatcherViewModel,
    onActivitySelectorClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = viewModel.selectedApp?.name ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = viewModel.selectedApp?.packageName ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Patch Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        SelectionColumn(modifier = Modifier.fillMaxWidth()) {
            SelectionItem(
                selected = viewModel.patchConfig.mode == PatchMode.CONSTRUCTOR,
                onClick = { viewModel.patchConfig = viewModel.patchConfig.copy(mode = PatchMode.CONSTRUCTOR) },
                icon = Icons.Outlined.Construction,
                title = "Constructor",
                desc = "Inject at <init>",
                extraContent = if (viewModel.patchConfig.mode == PatchMode.CONSTRUCTOR) {
                    {
                        TextButton(onClick = onActivitySelectorClick) {
                            Text("Select Activity")
                        }
                    }
                } else null
            )

            SelectionItem(
                selected = viewModel.patchConfig.mode == PatchMode.ACTIVITY_ENTRY,
                onClick = { viewModel.patchConfig = viewModel.patchConfig.copy(mode = PatchMode.ACTIVITY_ENTRY) },
                icon = Icons.Outlined.Api,
                title = "Activity Entry",
                desc = "Inject at onCreate",
                extraContent = if (viewModel.patchConfig.mode == PatchMode.ACTIVITY_ENTRY) {
                    {
                        TextButton(onClick = onActivitySelectorClick) {
                            Text("Select Activity")
                        }
                    }
                } else null
            )
        }

        Text(
            text = "Options",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        SettingsCheckBox(
            modifier = Modifier.clickable {
                viewModel.patchConfig = viewModel.patchConfig.copy(debuggable = !viewModel.patchConfig.debuggable)
            },
            checked = viewModel.patchConfig.debuggable,
            icon = Icons.Outlined.BugReport,
            title = "Debuggable",
            desc = "Sets android:debuggable=\"true\" in manifest"
        )

        SettingsCheckBox(
            modifier = Modifier.clickable {
                viewModel.patchConfig = viewModel.patchConfig.copy(overrideVersionCode = !viewModel.patchConfig.overrideVersionCode)
            },
            checked = viewModel.patchConfig.overrideVersionCode,
            icon = Icons.Outlined.Numbers,
            title = "Override Version Code",
            desc = "Sets android:versionCode=\"1\" in manifest"
        )
    }
}

@Composable
private fun SettingsCheckBox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    icon: ImageVector,
    title: String,
    desc: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (desc != null) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun PatchingView(viewModel: PatcherViewModel) {
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
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.dispatch(PatcherViewModel.ViewAction.Reset)
                        }
                    ) {
                        Text("Return")
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
        title = { Text("Select APK Source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStorageClick),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Select from storage")
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAppListClick),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Apps, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Select from installed apps")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InstalledAppsDialog(
    context: Context,
    onDismiss: () -> Unit,
    onAppSelected: (String, String, String) -> Unit
) {
    val installedApps = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.sourceDir != null }
            .map { appInfo ->
                Triple(
                    appInfo.packageName,
                    pm.getApplicationLabel(appInfo).toString(),
                    appInfo.sourceDir
                )
            }
            .sortedBy { it.second }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column {
                Text(
                    text = "Select App",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn {
                    items(installedApps) { (packageName, appName, apkPath) ->
                        ListItem(
                            headlineContent = { Text(appName) },
                            supportingContent = { Text(packageName) },
                            modifier = Modifier.clickable {
                                onAppSelected(packageName, appName, apkPath)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
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
        title = { Text("Select Activity") },
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
                Text("Close")
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
                .animateContentSize(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
        ) {
            ShimmerAnimation(
                enabled = patchState == PatcherState.PATCHING
            ) { brush ->
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
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

                    // Auto-scroll to bottom
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            scrollState.animateScrollToItem(logs.size - 1)
                        }
                    }
                }
            }
        }
    }
}
