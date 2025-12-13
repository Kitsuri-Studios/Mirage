package io.kitsuri.m1rage.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.kitsuri.m1rage.navigation.Screen

@Composable
fun BottomNavBar(
    screens: List<Screen>,
    selectedScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        NavigationBar {
            screens.forEach { screen ->
                BottomNavItem(
                    screen = screen,
                    isSelected = selectedScreen == screen,
                    onClick = { onScreenSelected(screen) }
                )
            }
        }
    }
}