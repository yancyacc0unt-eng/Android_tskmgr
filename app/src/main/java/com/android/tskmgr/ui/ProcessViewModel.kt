package com.android.tskmgr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.tskmgr.data.PermissionsHelper
import com.android.tskmgr.data.ProcessInfo
import com.android.tskmgr.data.ProcessRepository
import com.android.tskmgr.data.RecentAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ProcessUiState(
    val processes: List<ProcessInfo> = emptyList(),
    val recentApps: List<RecentAppInfo> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val loading: Boolean = true,
    val snackbar: String? = null,
)

class ProcessViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProcessRepository(app)

    private val _state = MutableStateFlow(ProcessUiState())
    val state: StateFlow<ProcessUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val running = repo.getRunningProcesses()
                val runningNames = running.map { it.processName }.toSet()
                val hasAccess = PermissionsHelper.hasUsageAccess(getApplication())
                val recent = if (hasAccess) repo.getRecentApps(runningNames) else emptyList()
                _state.value = ProcessUiState(
                    processes = running,
                    recentApps = recent,
                    hasUsageAccess = hasAccess,
                    loading = false,
                )
                delay(2000)
            }
        }
    }

    fun killProcess(process: ProcessInfo) {
        val result = repo.killProcess(process.processName, process.pid)
        _state.value = _state.value.copy(
            snackbar = when (result) {
                ProcessRepository.KillResult.SUCCESS -> "Stopped ${process.appLabel}"
                ProcessRepository.KillResult.PERMISSION_DENIED -> "Insufficient permissions to stop ${process.appLabel}"
                ProcessRepository.KillResult.FAILED -> "Could not stop ${process.appLabel}"
            },
        )
    }

    fun consumeSnackbar() {
        _state.value = _state.value.copy(snackbar = null)
    }
}
