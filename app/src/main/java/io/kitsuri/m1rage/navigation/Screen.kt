package io.kitsuri.m1rage.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Patcher : Screen("patcher", "Patcher", Icons.Filled.Build, Icons.Outlined.Build)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}