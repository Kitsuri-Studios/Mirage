package io.kitsuri.m1rage.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// i am not sure if i am allowed to use enum class for ordinal. so i added ordinal to this class manually to fix transition animation
sealed class Screen(
    val ordinal: Int,
    val route: String,
    val title: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    object Home : Screen(0, "home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Patcher : Screen(1, "patcher", "Patcher", Icons.Filled.Build, Icons.Outlined.Build)
    object Settings : Screen(2, "settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}