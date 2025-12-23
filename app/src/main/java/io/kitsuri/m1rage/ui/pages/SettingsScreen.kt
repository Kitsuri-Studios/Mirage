package io.kitsuri.m1rage.ui.pages

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kitsuri.m1rage.activities.MainActivity
import io.kitsuri.m1rage.globals.AppContext
import io.kitsuri.m1rage.model.SettingItem
import io.kitsuri.m1rage.model.SettingType
import io.kitsuri.m1rage.model.SettingsManager
import io.kitsuri.m1rage.ui.components.TopBarConfig
import io.kitsuri.m1rage.utils.UniversalPainter


@Composable
internal fun SettingsScreen(settingsManager: SettingsManager, onTopBarConfigChanged: (TopBarConfig) -> Unit) {
    val scrollState = rememberScrollState()
    val settingStates = remember { mutableMapOf<String, MutableState<Any>>() }

    fun savePreference(key: String, value: Any) {
        when (value) {
            is Boolean -> settingsManager.setBooleanValue(key, value)
            is String -> settingsManager.setStringValue(key, value)
            is Float -> settingsManager.setFloatValue(key, value)
        }
    }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        if (settingsManager.settingsList.isEmpty()) {
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
                settingsManager.settingsList.forEach { setting ->
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

@Composable
fun SettingItemView(
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
    }
}

@Composable
fun TextFieldSettingItem(
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
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
                Text(
                    text = currentValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SwitchSettingItem(
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
                Text(
                    text = if (currentValue) "Enabled" else "Disabled",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
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
fun CheckboxSettingItem(
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
                Text(
                    text = if (currentValue) "Checked" else "Unchecked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
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
fun SliderSettingItem(
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
                Text(
                    text = "${currentValue.toInt()}${setting.suffix}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
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
fun ButtonSettingItem(setting: SettingItem, iconPainter: Painter) {
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

