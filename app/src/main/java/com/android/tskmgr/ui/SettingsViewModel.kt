package com.android.tskmgr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.tskmgr.data.PermissionsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(val hasUsageAccess: Boolean = false)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = SettingsUiState(hasUsageAccess = PermissionsHelper.hasUsageAccess(getApplication()))
    }
}
