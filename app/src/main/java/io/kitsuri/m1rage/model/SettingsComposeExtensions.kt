package io.kitsuri.m1rage.model

import androidx.compose.runtime.*

@Composable
fun SettingsManager.observeBooleanAsState(
    key: String,
    defaultValue: Boolean
): State<Boolean> {

    val state = remember {
        mutableStateOf(getBooleanValue(key, defaultValue))
    }

    DisposableEffect(key) {
        val listener = object : SettingsManager.SettingsChangeListener {
            override fun onSettingChanged(changedKey: String, value: Any) {
                if (changedKey == key && value is Boolean) {
                    state.value = value
                }
            }
        }

        addSettingsChangeListener(listener)
        onDispose { removeSettingsChangeListener(listener) }
    }

    return state
}


@Composable
fun SettingsManager.observeStringAsState(
    key: String,
    defaultValue: String
): State<String> {

    val state = remember {
        mutableStateOf(getStringValue(key, defaultValue))
    }

    DisposableEffect(key) {
        val listener = object : SettingsManager.SettingsChangeListener {
            override fun onSettingChanged(changedKey: String, value: Any) {
                if (changedKey == key && value is String) {
                    state.value = value
                }
            }
        }

        addSettingsChangeListener(listener)

        onDispose {
            removeSettingsChangeListener(listener)
        }
    }

    return state
}
