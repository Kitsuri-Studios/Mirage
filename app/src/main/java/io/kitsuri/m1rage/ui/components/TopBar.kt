package io.kitsuri.m1rage.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import io.kitsuri.m1rage.ui.theme.MFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(title: String) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontFamily = MFontFamily) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}