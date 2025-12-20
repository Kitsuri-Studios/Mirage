package io.kitsuri.m1rage.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.kitsuri.m1rage.utils.HxoLogReader
import io.kitsuri.m1rage.utils.PatchedAppScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsViewModel : ViewModel() {

    private val _appsWithLogs = MutableStateFlow<List<AppLogInfo>>(emptyList())
    val appsWithLogs: StateFlow<List<AppLogInfo>> = _appsWithLogs.asStateFlow()

    private val _logContent = MutableStateFlow<String>("")
    val logContent: StateFlow<String> = _logContent.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppLogInfo?>(null)
    val selectedApp: StateFlow<AppLogInfo?> = _selectedApp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAppsWithLogs(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                val patchedApps = PatchedAppScanner.scanPatchedApps(context)
                HxoLogReader.scanForLogs(patchedApps)
            }
            _appsWithLogs.value = apps
            _isLoading.value = false
        }
    }

    fun loadLogContent(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val content = withContext(Dispatchers.IO) {
                HxoLogReader.readLogFile(packageName)
            }
            _logContent.value = content ?: ""
            _selectedApp.value = _appsWithLogs.value.find { it.packageName == packageName }
            _isLoading.value = false
        }
    }

    fun clearSelection() {
        _selectedApp.value = null
        _logContent.value = ""
    }
}