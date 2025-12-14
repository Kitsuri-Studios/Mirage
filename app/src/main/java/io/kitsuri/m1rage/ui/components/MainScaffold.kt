package io.kitsuri.m1rage.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.kitsuri.m1rage.navigation.Screen
import io.kitsuri.m1rage.ui.pages.HomeScreen
import io.kitsuri.m1rage.ui.pages.PatcherScreen
import io.kitsuri.m1rage.ui.pages.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var oldSelectedScreenInt = Screen.Home.ordinal // to remember old screen (for fixing transition)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val screens = listOf(Screen.Home, Screen.Patcher, Screen.Settings)
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current // import the class

    LaunchedEffect(selectedScreen) { // update the variable on selectedScreen change
        oldSelectedScreenInt = selectedScreen.ordinal
    }

    BackHandler {
        if (selectedScreen != Screen.Home) {
            selectedScreen = Screen.Home
        } else {
            if (backPressedOnce) {
                activity?.finish()
            } else {
                backPressedOnce = true
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Press back again to exit",
                        duration = SnackbarDuration.Short
                    )
                    delay(2000)
                    backPressedOnce = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(title = selectedScreen.title)
        },
        bottomBar = {
            BottomNavBar(
                screens = screens,
                selectedScreen = selectedScreen,
                onScreenSelected = { selectedScreen = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(selectedScreen = selectedScreen)
        }
    }
}