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

data class PerformanceUiState(
    val cpuPercent: Double = 0.0,
    val cpuHistory: List<Double> = emptyList(),
    val cores: List<Double> = emptyList(),
    val memory: SystemMetrics.MemoryInfo? = null,
    val storage: SystemMetrics.StorageInfo? = null,
    val rxHistory: List<Long> = emptyList(),
    val txHistory: List<Long> = emptyList(),
)

/** Drives the Performance tab, sampling every second. */
class PerformanceViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PerformanceUiState())
    val state: StateFlow<PerformanceUiState> = _state.asStateFlow()

    private val historyCap = 60 // ~1 minute of 1s samples
    private var lastRx = 0L
    private var lastTx = 0L

    init {
        viewModelScope.launch {
            while (isActive) {
                val cpu = SystemMetrics.readCpuUsage()
                val memory = SystemMetrics.readMemory(getApplication())
                val storage = SystemMetrics.readStorage()
                val net = SystemMetrics.readNetwork(lastRx, lastTx)
                lastRx = lastRx + net.rxBytesPerSec
                lastTx = lastTx + net.txBytesPerSec

                _state.value = _state.value.copy(
                    cpuPercent = cpu?.totalPercent ?: 0.0,
                    cpuHistory = append(_state.value.cpuHistory, cpu?.totalPercent ?: 0.0),
                    cores = cpu?.perCore?.drop(1) ?: emptyList(),
                    memory = memory,
                    storage = storage,
                    rxHistory = append(_state.value.rxHistory, net.rxBytesPerSec),
                    txHistory = append(_state.value.txHistory, net.txBytesPerSec),
                )
                delay(1000)
            }
        }
    }

    private fun <T> append(list: List<T>, value: T): List<T> =
        (list + value).takeLast(historyCap)
}
