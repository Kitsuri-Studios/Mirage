package io.kitsuri.m1rage.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import io.kitsuri.m1rage.ui.components.MainScaffold
import io.kitsuri.m1rage.ui.theme.M1rageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            M1rageTheme() {
                MainScaffold()
            }
        }
    }
}