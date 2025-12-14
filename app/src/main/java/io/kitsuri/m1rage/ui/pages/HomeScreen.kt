package io.kitsuri.m1rage.ui.pages


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kitsuri.m1rage.navigation.HomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// use most private visibility modifier where possible - fzul
internal fun HomeScreen() {
    // Secondary tab row
    val startDestination = HomeScreen.HomeScreenA
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    SecondaryTabRow(selectedTabIndex = selectedDestination) {
        HomeScreen.entries.forEachIndexed { index, destination ->
            Tab(
                selected = selectedDestination == index,
                onClick = { selectedDestination = index },
                text = { Text(destination.title) }
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "Home",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Home",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
