package io.kitsuri.m1rage.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kitsuri.m1rage.model.PatchedAppInfo
import io.kitsuri.m1rage.model.AppSettingsManager
import io.kitsuri.m1rage.model.SettingItem
import io.kitsuri.m1rage.model.SettingType
import io.kitsuri.m1rage.ui.pages.MultiChoiceSettingRow
import io.kitsuri.m1rage.ui.pages.SettingsDivider
import io.kitsuri.m1rage.utils.AppLibraryUtils
import io.kitsuri.m1rage.utils.UniversalPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    app: PatchedAppInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { AppSettingsManager(context, app.packageName) }
    val settingsList = settingsManager.settingsList
    val scrollState = rememberScrollState()
    val settingStates = remember { mutableMapOf<String, MutableState<Any>>() }

    fun savePreference(key: String, value: Any) {
        when (value) {
            is Boolean -> settingsManager.setBooleanValue(key, value)
            is String -> settingsManager.setStringValue(key, value)
            is Float -> settingsManager.setFloatValue(key, value)
        }
        // Auto-update hxo.ini when any setting changes
        AppLibraryUtils.writeIniFile(context, app.packageName, settingsManager)
    }

    // Initialize default settings if empty
    LaunchedEffect(Unit) {
        if (settingsList.isEmpty()) {
            // [HXO] Section
            settingsManager.addInfo(
                key = "hxo_section_header",
                title = "HXO Loader Configuration",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_settings
            )

            settingsManager.addSwitch(
                key = "hxo_enabled",
                title = "Enable HXO Loader",
                defaultValue = true,
                description = "Enable or disable HXO loader",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_toggle_right
            )

            settingsManager.addSlider(
                key = "load_delay",
                title = "Load Delay (sleep)",
                defaultValue = 0f,
                minValue = 0f,
                maxValue = 30f,
                suffix = "s",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_clock
            )

            settingsManager.addSwitch(
                key = "unload_after_execution",
                title = "Unload After Execution",
                defaultValue = false,
                description = "EXPERIMENTAL: Unload library after execution",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_alert_triangle
            )

            // [1337] Section
            settingsManager.addInfo(
                key = "advanced_section_header",
                title = "Advanced Settings (EXPERIMENTAL)",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_code
            )

            settingsManager.addTextField(
                key = "lib_path",
                title = "Library Path (lib)",
                defaultValue = "/usr/lib/",
                description = "Default HXO rpath - DON'T MODIFY",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_folder
            )

            // Info Section
            settingsManager.addInfo(
                key = "config_location",
                title = "Config: /Android/media/${app.packageName}/",
                customIconResId = ir.alirezaivaz.tablericons.R.drawable.ic_info_circle
            )

            // Initialize files
            AppLibraryUtils.initializeAppFiles(context, app.packageName, settingsManager)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${app.appName} - Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            if (settingsList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No settings configured",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    settingsList.forEach { setting ->
                        val state = settingStates.getOrPut(setting.key) {
                            val savedValue = when (setting.type) {
                                SettingType.SWITCH, SettingType.CHECKBOX -> {
                                    settingsManager.getBooleanValue(setting.key, setting.defaultValue as Boolean)
                                }
                                SettingType.SLIDER -> {
                                    settingsManager.getFloatValue(setting.key, setting.defaultValue as Float)
                                }
                                SettingType.TEXT_FIELD -> {
                                    settingsManager.getStringValue(setting.key, setting.defaultValue as String)
                                }
                                else -> setting.defaultValue
                            }
                            mutableStateOf(savedValue)
                        }

                        SettingItemView(
                            setting = setting,
                            currentValue = state.value,
                            onValueChange = { newValue ->
                                state.value = newValue
                                if (setting.type != SettingType.INFO && setting.type != SettingType.BUTTON) {
                                    savePreference(setting.key, newValue)
                                }
                            },
                            iconPainter = UniversalPainter(setting.customIconResId ?: ir.alirezaivaz.tablericons.R.drawable.ic_settings)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItemView(
    setting: SettingItem,
    currentValue: Any,
    onValueChange: (Any) -> Unit,
    iconPainter: Painter
) {
    when (setting.type) {
        SettingType.TEXT_FIELD -> {
            TextFieldSettingItem(
                setting = setting,
                currentValue = currentValue as String,
                onValueChange = onValueChange,
                iconPainter = iconPainter
            )
        }

        SettingType.SWITCH -> {
            SwitchSettingItem(
                setting = setting,
                currentValue = currentValue as Boolean,
                onValueChange = onValueChange,
                iconPainter = iconPainter
            )
        }

        SettingType.CHECKBOX -> {
            CheckboxSettingItem(
                setting = setting,
                currentValue = currentValue as Boolean,
                onValueChange = onValueChange,
                iconPainter = iconPainter
            )
        }

        SettingType.SLIDER -> {
            SliderSettingItem(
                setting = setting,
                currentValue = currentValue as Float,
                onValueChange = onValueChange,
                iconPainter = iconPainter
            )
        }

        SettingType.BUTTON -> {
            ButtonSettingItem(setting = setting, iconPainter = iconPainter)
        }

        SettingType.INFO -> {
            if (setting.title.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = setting.title,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
        SettingType.MULTI_CHOICE -> {
            MultiChoiceSettingRow(item = setting)
        }

        SettingType.DIVIDER -> SettingsDivider()
    }
}

@Composable
private fun TextFieldSettingItem(
    setting: SettingItem,
    currentValue: String,
    onValueChange: (Any) -> Unit,
    iconPainter: Painter
) {
    val showDialog = remember { mutableStateOf(false) }
    val tempValue = remember { mutableStateOf(currentValue) }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(setting.title) },
            text = {
                OutlinedTextField(
                    value = tempValue.value,
                    onValueChange = { tempValue.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = setting.isReadOnly,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(tempValue.value)
                        showDialog.value = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!setting.isReadOnly) showDialog.value = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = setting.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (setting.description != null) {
                    Text(
                        text = setting.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = currentValue,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingItem(
    setting: SettingItem,
    currentValue: Boolean,
    onValueChange: (Any) -> Unit,
    iconPainter: Painter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = setting.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (setting.description != null) {
                    Text(
                        text = setting.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = if (currentValue) "Enabled" else "Disabled",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Switch(
            checked = currentValue,
            onCheckedChange = { onValueChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun CheckboxSettingItem(
    setting: SettingItem,
    currentValue: Boolean,
    onValueChange: (Any) -> Unit,
    iconPainter: Painter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = setting.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (setting.description != null) {
                    Text(
                        text = setting.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = if (currentValue) "Checked" else "Unchecked",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Checkbox(
            checked = currentValue,
            onCheckedChange = { onValueChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderSettingItem(
    setting: SettingItem,
    currentValue: Float,
    onValueChange: (Any) -> Unit,
    iconPainter: Painter
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = setting.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (setting.description != null) {
                    Text(
                        text = setting.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "${currentValue.toInt()}${setting.suffix}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Slider(
            value = currentValue,
            onValueChange = { onValueChange(it) },
            valueRange = setting.minValue..setting.maxValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier.size(16.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        )
    }
}

@Composable
private fun ButtonSettingItem(setting: SettingItem, iconPainter: Painter) {
    Button(
        onClick = { setting.onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = setting.title,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}