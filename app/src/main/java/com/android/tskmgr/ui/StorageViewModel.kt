package com.android.tskmgr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.tskmgr.data.SystemMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StorageViewModel(app: Application) : AndroidViewModel(app) {
    private val _storage = MutableStateFlow<SystemMetrics.StorageInfo?>(null)
    val storage: StateFlow<SystemMetrics.StorageInfo?> = _storage.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _storage.value = SystemMetrics.readStorage()
                delay(3000)
            }
        }
    }
}
