package io.kitsuri.m1rage.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kitsuri.m1rage.model.HomeViewModel
import io.kitsuri.m1rage.model.LogsViewModel
import io.kitsuri.m1rage.model.PatchedAppInfo
import io.kitsuri.m1rage.navigation.HomeScreen
import io.kitsuri.m1rage.ui.components.TopBarConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    onTopBarConfigChanged: (TopBarConfig) -> Unit
) {
    val homeViewModel = viewModel<HomeViewModel>()
    val logsViewModel = viewModel<LogsViewModel>()
    val context = LocalContext.current
    val patchedApps by homeViewModel.patchedApps.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.refreshPatchedApps(context)
        // Show default top bar for Home screen
        onTopBarConfigChanged(TopBarConfig(show = true))
    }

    var selectedDestination by remember { mutableIntStateOf(HomeScreen.HomeScreenA.ordinal) }
    var previousDestination by remember { mutableIntStateOf(selectedDestination) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedDestination) {
            HomeScreen.entries.forEachIndexed { index, destination ->
                Tab(
                    selected = selectedDestination == index,
                    onClick = {
                        previousDestination = selectedDestination
                        selectedDestination = index
                    },
                    text = { Text(destination.title) }
                )
            }
        }

        AnimatedContent(
            targetState = selectedDestination,
            transitionSpec = {
                val direction = if (targetState > previousDestination) 1 else -1
                slideInHorizontally { width -> direction * width } togetherWith
                        slideOutHorizontally { width -> -direction * width }
            },
            label = "tab_transition"
        ) { destination ->
            when (destination) {
                HomeScreen.HomeScreenA.ordinal -> {
                    when {
                        isLoading -> LoadingView()
                        patchedApps.isEmpty() -> EmptyStateView()
                        else -> PatchedAppsListView(patchedApps = patchedApps)
                    }
                }
                HomeScreen.Logs.ordinal -> {
                    LogsTab(viewModel = logsViewModel)
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Apps,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No patched apps found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Patched apps will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PatchedAppsListView(patchedApps: List<PatchedAppInfo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(patchedApps, key = { it.packageName }) { app ->
            PatchedAppCard(app = app)
        }
    }
}

@Composable
private fun PatchedAppCard(app: PatchedAppInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}