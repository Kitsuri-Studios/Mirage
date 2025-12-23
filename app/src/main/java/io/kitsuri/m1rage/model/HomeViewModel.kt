package io.kitsuri.m1rage.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val _patchedApps = MutableStateFlow<List<PatchedAppInfo>>(emptyList())
    val patchedApps: StateFlow<List<PatchedAppInfo>> = _patchedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refreshPatchedApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                PatchedAppScanner.scanPatchedApps(context)
            }
            _patchedApps.value = apps
            _isLoading.value = false
        }
    }
}