package io.kitsuri.m1rage.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kitsuri.m1rage.model.AppSettingsManager
import io.kitsuri.m1rage.model.SettingItem
import io.kitsuri.m1rage.model.SettingType

@Composable
fun SettingItemView(
    setting: SettingItem,
    settingsManager: AppSettingsManager
) {
    when (setting.type) {
        SettingType.SWITCH -> SwitchSettingItem(setting, settingsManager)
        SettingType.CHECKBOX -> CheckboxSettingItem(setting, settingsManager)
        SettingType.SLIDER -> SliderSettingItem(setting, settingsManager)
        SettingType.TEXT_FIELD -> TextFieldSettingItem(setting, settingsManager)
        SettingType.BUTTON -> ButtonSettingItem(setting)
        SettingType.INFO -> InfoSettingItem(setting)
    }
}

@Composable
private fun SwitchSettingItem(
    setting: SettingItem,
    settingsManager: AppSettingsManager
) {
    var checked by remember {
        mutableStateOf(
            settingsManager.getBooleanValue(
                setting.key,
                setting.defaultValue as? Boolean ?: false
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                setting.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    settingsManager.setBooleanValue(setting.key, it)
                }
            )
        }
    }
}

@Composable
private fun CheckboxSettingItem(
    setting: SettingItem,
    settingsManager: AppSettingsManager
) {
    var checked by remember {
        mutableStateOf(
            settingsManager.getBooleanValue(
                setting.key,
                setting.defaultValue as? Boolean ?: false
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                setting.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    settingsManager.setBooleanValue(setting.key, it)
                }
            )
        }
    }
}

@Composable
private fun SliderSettingItem(
    setting: SettingItem,
    settingsManager: AppSettingsManager
) {
    var value by remember {
        mutableFloatStateOf(
            settingsManager.getFloatValue(
                setting.key,
                setting.defaultValue as? Float ?: 0f
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setting.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    setting.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${value.toInt()}${setting.suffix}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = {
                    settingsManager.setFloatValue(setting.key, value)
                },
                valueRange = setting.minValue..setting.maxValue
            )
        }
    }
}

@Composable
private fun TextFieldSettingItem(
    setting: SettingItem,
    settingsManager: AppSettingsManager
) {
    var text by remember {
        mutableStateOf(
            settingsManager.getStringValue(
                setting.key,
                setting.defaultValue as? String ?: ""
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            setting.description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    settingsManager.setStringValue(setting.key, it)
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = setting.isReadOnly,
                singleLine = true
            )
        }
    }
}

@Composable
private fun ButtonSettingItem(setting: SettingItem) {
    Button(
        onClick = { setting.onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = setting.buttonColor
        )
    ) {
        Text(
            text = setting.title,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun InfoSettingItem(setting: SettingItem) {
    if (setting.title.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = setting.title,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}