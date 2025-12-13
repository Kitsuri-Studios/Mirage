package io.kitsuri.m1rage.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import io.kitsuri.m1rage.navigation.Screen

@Composable
fun RowScope.BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        icon = {
            Icon(
                if (isSelected) screen.iconFilled else screen.iconOutlined,
                contentDescription = screen.title
            )
        },
        label = {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(200)) +
                        expandVertically(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)) +
                        shrinkVertically(animationSpec = tween(200))
            ) {
                Text(screen.title)
            }
        },
        selected = isSelected,
        onClick = onClick
    )
}