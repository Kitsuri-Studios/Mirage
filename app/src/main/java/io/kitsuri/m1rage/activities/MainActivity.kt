package io.kitsuri.m1rage.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import io.kitsuri.m1rage.globals.AppContext
import io.kitsuri.m1rage.ui.components.MainScaffold
import io.kitsuri.m1rage.ui.theme.M1rageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerSetting()
        setContent {
            M1rageTheme() {
                MainScaffold()
            }
        }
    }
    fun registerSetting(){

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