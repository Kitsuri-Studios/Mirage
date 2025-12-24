package io.kitsuri.m1rage.ui.pages

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kitsuri.m1rage.ui.components.AppItem
import io.kitsuri.m1rage.ui.components.SearchAppBar
import io.kitsuri.m1rage.ui.components.TopBarConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSplit: Boolean,
    val splitApkPaths: List<String>,
    val appIcon: ImageBitmap
)

class SelectAppsViewModel : ViewModel() {
    var allApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var filteredApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    fun loadApps(context: Context) {
        viewModelScope.launch {
            isLoading = true

            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { appInfo ->
                        // Exclude system apps
                        appInfo.sourceDir != null &&
                                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                    }
                    .mapNotNull { appInfo ->
                        try {
                            val splitDirs = appInfo.splitSourceDirs
                            val isSplit = splitDirs != null && splitDirs.isNotEmpty()
                            val apkPaths = if (isSplit) {
                                listOf(appInfo.sourceDir) + splitDirs.toList()
                            } else {
                                listOf(appInfo.sourceDir)
                            }

                            // Get app label (not package name)
                            val appName = try {
                                appInfo.loadLabel(pm).toString()
                            } catch (e: Exception) {
                                appInfo.packageName
                            }

                            // Safely get icon and convert to fixed size bitmap
                            val icon = try {
                                pm.getApplicationIcon(appInfo.packageName)
                            } catch (e: Exception) {
                                null
                            }

                            val iconBitmap = icon?.let { drawable ->
                                try {
                                    // Convert to fixed 48dp size (will be displayed at 24dp in UI)
                                    drawable.toBitmap(48, 48, Bitmap.Config.ARGB_8888)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (iconBitmap != null) {
                                AppInfo(
                                    packageName = appInfo.packageName,
                                    appName = appName,
                                    isSplit = isSplit,
                                    splitApkPaths = apkPaths,
                                    appIcon = iconBitmap.asImageBitmap()
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .sortedBy { it.appName.lowercase() }
            }

            allApps = apps
            filteredApps = apps
            isLoading = false
        }
    }

    fun filterApps(searchText: String) {
        filteredApps = if (searchText.isBlank()) {
            allApps
        } else {
            val lowerSearch = searchText.toLowerCase(Locale.current)
            allApps.filter {
                it.appName.toLowerCase(Locale.current).contains(lowerSearch) ||
                        it.packageName.toLowerCase(Locale.current).contains(lowerSearch)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppsScreen(
    onAppSelected: (String, String, Boolean, List<String>) -> Unit,
    onBackClick: () -> Unit,
    onTopBarConfigChanged: (TopBarConfig) -> Unit
) {
    val viewModel = viewModel<SelectAppsViewModel>()
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }

    // Set custom top bar for this screen
    LaunchedEffect(Unit) {
        viewModel.loadApps(context)

        onTopBarConfigChanged(
            TopBarConfig(
                show = true,
                content = {
                    SearchAppBar(
                        title = { Text("Select App") },
                        searchText = searchText,
                        onSearchTextChange = {
                            searchText = it
                            viewModel.filterApps(it)
                        },
                        onClearClick = {
                            searchText = ""
                            viewModel.filterApps("")
                        },
                        onBackClick = onBackClick
                    )
                }
            )
        )
    }

    // Update top bar when searchText changes
    LaunchedEffect(searchText) {
        onTopBarConfigChanged(
            TopBarConfig(
                show = true,
                content = {
                    SearchAppBar(
                        title = { Text("Select App") },
                        searchText = searchText,
                        onSearchTextChange = {
                            searchText = it
                            viewModel.filterApps(it)
                        },
                        onClearClick = {
                            searchText = ""
                            viewModel.filterApps("")
                        },
                        onBackClick = onBackClick
                    )
                }
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading apps...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            AppList(
                apps = viewModel.filteredApps,
                onAppClick = { app ->
                    onAppSelected(
                        app.packageName,
                        app.appName,
                        app.isSplit,
                        app.splitApkPaths
                    )
                }
            )
        }
    }
}

@Composable
private fun AppList(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            AppItem(
                modifier = Modifier
                    .clickable { onAppClick(app) },
                icon = app.appIcon,
                label = app.appName,
                packageName = app.packageName,
                additionalContent = if (app.isSplit) {
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Split",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${app.splitApkPaths.size} APK files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else null
            )
        }
        item {
            Spacer(Modifier.height(64.dp))
        }
    }
}
