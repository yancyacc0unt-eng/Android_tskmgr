package com.android.tskmgr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.tskmgr.data.ProcessInfo
import com.android.tskmgr.data.ProcessRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ProcessUiState(
    val processes: List<ProcessInfo> = emptyList(),
    val loading: Boolean = true,
    val snackbar: String? = null,
)

class ProcessViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProcessRepository(app)

    private val _state = MutableStateFlow(ProcessUiState())
    val state: StateFlow<ProcessUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val list = repo.getRunningProcesses()
                _state.value = ProcessUiState(processes = list, loading = false)
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
