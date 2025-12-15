package io.kitsuri.m1rage.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import io.kitsuri.m1rage.navigation.Screen
import io.kitsuri.m1rage.ui.pages.HomeScreen
import io.kitsuri.m1rage.ui.pages.PatcherScreen
import io.kitsuri.m1rage.ui.pages.SettingsScreen

@Composable
fun NavHost(
    selectedScreen: Screen,
    onConfigureStateChanged: (Boolean) -> Unit
) {
    val isLeftTab = oldSelectedScreenInt < selectedScreen.ordinal

    AnimatedContent(
        targetState = selectedScreen,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    if (isLeftTab) fullWidth else -fullWidth
                },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth ->
                            if (!isLeftTab) fullWidth else -fullWidth
                        },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
        },
        label = "screen_transition"
    ) { targetScreen ->
        when (targetScreen) {
            Screen.Home -> {
                onConfigureStateChanged(false)
                HomeScreen()
            }

            Screen.Patcher -> {
                PatcherScreen(
                    onConfigureStateChanged = onConfigureStateChanged
                )
            }

            Screen.Settings -> {
                onConfigureStateChanged(false)
                SettingsScreen()
            }
        }
    }
}
