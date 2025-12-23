
package io.kitsuri.m1rage.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import io.kitsuri.m1rage.globals.AppContext
import io.kitsuri.m1rage.utils.CleanupManager
import io.kitsuri.m1rage.ui.components.MainScaffold
import io.kitsuri.m1rage.ui.pages.RepairModeScreen
import io.kitsuri.m1rage.ui.theme.M1rageTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val cleanupLogs = mutableStateListOf<String>()
    private var isCleaning by mutableStateOf(false)
    private var needsRepair by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        registerSetting()

        setContent {
            M1rageTheme {
                var showRepairScreen by remember { mutableStateOf(false) }
                var repairProgress by remember { mutableStateOf(0f) }
                var repairPercentage by remember { mutableStateOf(0) }
                val repairLogs = remember { mutableStateListOf<Pair<Int, String>>() }

                LaunchedEffect(Unit) {
                    val hasWorkspaces = CleanupManager.hasOldWorkspaces(this@MainActivity)
                    val hasTempFiles = CleanupManager.hasTempCacheFiles(this@MainActivity)

                    if (hasWorkspaces || hasTempFiles) {
                        showRepairScreen = true
                        isCleaning = true
                        repairLogs.clear()
                        repairProgress = 0f
                        repairPercentage = 0

                        repairLogs.add(Log.INFO to "Starting system repair...")

                        var step = 0
                        val totalSteps = (if (hasWorkspaces) 2 else 0) + (if (hasTempFiles) 1 else 0)

                        if (hasWorkspaces) {
                            step++
                            repairPercentage = (step * 100) / totalSteps
                            repairProgress = repairPercentage / 100f
                            repairLogs.add(Log.INFO to "Removing old workspaces...")
                            CleanupManager.cleanupWorkspaces(this@MainActivity) { log ->
                                repairLogs.add(Log.INFO to log)
                            }
                            delay(300)
                        }

                        if (hasTempFiles) {
                            step++
                            repairPercentage = (step * 100) / totalSteps
                            repairProgress = repairPercentage / 100f
                            repairLogs.add(Log.INFO to "Clearing temporary files...")
                            CleanupManager.cleanupTempCache(this@MainActivity) { log ->
                                repairLogs.add(Log.INFO to log)
                            }
                            delay(300)
                        }

                        repairLogs.add(Log.INFO to "Repair completed successfully!")
                        repairPercentage = 100
                        repairProgress = 1f
                        isCleaning = false
                    }
                }

                if (showRepairScreen) {
                    RepairModeScreen(
                        progress = repairProgress,
                        progressPercentage = repairPercentage,
                        logs = repairLogs,
                        isCleaning = isCleaning,
                        onCleanupComplete = { showRepairScreen = false }
                    )
                } else {
                    MainScaffold()
                }
            }
        }
    }

    private fun registerSetting() {
        val settingsManager = AppContext.settingsManager

        settingsManager.addSwitch(
            key = "test_key",
            title = "TestSettings",
            defaultValue = true,
            description = "Enable or disable Test",
            customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_toggle_right
        )
    }
}