package io.kitsuri.m1rage.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kitsuri.m1rage.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var oldSelectedScreenInt = Screen.Home.ordinal

data class TopBarConfig(
    val show: Boolean = true,
    val content: (@Composable () -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Patcher) }

    val screens = listOf(Screen.Home, Screen.Patcher, Screen.Settings)

    var topBarConfig by remember { mutableStateOf(TopBarConfig()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(selectedScreen) {
        oldSelectedScreenInt = selectedScreen.ordinal
    }

    BackHandler {
        if (selectedScreen != Screen.Patcher) {
            selectedScreen = Screen.Patcher
        } else {
            if (backPressedOnce) {
                activity?.finish()
            } else {
                backPressedOnce = true
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Press back again to exit",
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
            if (topBarConfig.show) {
                topBarConfig.content?.invoke() ?: TopBar(title = selectedScreen.title)
            }
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
                .padding(   top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() - 40.dp)
        ) {
            NavHost(
                selectedScreen = selectedScreen,
                onTopBarConfigChanged = { topBarConfig = it }
            )
        }
    }
}